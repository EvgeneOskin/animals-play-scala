// See the LICENCE file distributed with this work for licence info.
package models

import java.math.BigDecimal
import java.sql.{Types, Connection, PreparedStatement}
import scala.util.{Try, Success, Failure}
import securesocial.core._
import securesocial.core.providers.{ UsernamePasswordProvider }

case class User(main: BasicProfile, identities: List[BasicProfile])

object User {

  val baseQuery: String = """
  select
    UserProfile.providerId,
    UserProfile.userId,
    UserProfile.firstName,
    UserProfile.lastName,
    UserProfile.fullName,
    UserProfile.email,
    UserProfile.avatarUrl,
    UserProfile.authMethod,
    UserProfile.oAuth1InfoId,
    UserProfile.oAuth2InfoId,
    UserProfile.passwordInfoId,

    OAuth1Info.token,
    OAuth1Info.secret,

    OAuth2Info.accessToken,
    OAuth2Info.tokenType,
    OAuth2Info.expiresIn,
    OAuth2Info.refreshToken,

    PasswordInfo.hasher,
    PasswordInfo.password,
    PasswordInfo.salt

  from UserProfile
  join OAuth1Info on UserProfile.oAuth1InfoId = OAuth1Info.id
  join OAuth2Info on UserProfile.oAuth2InfoId = OAuth2Info.id
  join PasswordInfo on UserProfile.passwordInfoId = PasswordInfo.id
  """

  def find(providerId: String, userId: String, baseFindQuery: String = baseQuery): Option[BasicProfile] = {
    findUserProfile { conn =>
      val query = s"""
        $baseFindQuery
        where UserProfile.providerId = ? && UserProfile.userId = ?
        limit 1
      """
      val prep = conn.prepareStatement(query)
      JDBCHelper.setValue(prep, 1, providerId)
      JDBCHelper.setValue(prep, 2, userId)
      prep
    }
  }

  def findByEmailAndProvider(email: String, providerId: String): Option[BasicProfile] = {
    findUserProfile { conn =>
      val query = s"""
        $baseQuery
        where UserProfile.providerId = ? && UserProfile.email = ?
        limit 1
      """
      val prep = conn.prepareStatement(query)
      JDBCHelper.setValue(prep, 1, providerId)
      JDBCHelper.setValue(prep, 2, email)
      prep
    }
  }

  def save(entry: BasicProfile): BasicProfile = {
    import play.api.db._
    import play.api.Play.current

    var insertedId: String = ""
    DB.withConnection { conn =>
      insertUserProfile(conn, entry) match {
        case Success(id: Int) => insertedId = id.toString
        case Failure(e) => throw e
      }
    }
    entry
  }

  def updatePasswordInfo(profile: BasicProfile, entry: PasswordInfo): Try[Int] = {
    import play.api.db._
    import play.api.Play.current

    var updatedRowsCount = 0
    DB.withConnection { conn =>
      val id: Try[Int] = getPasswordInfoId(conn, profile.userId)
      updatePasswordInfo(conn, id, entry) match {
        case Success(updatedRows) => updatedRowsCount = updatedRows
        case Failure(e) => throw e
      }
    }
    updatedRowsCount match {
      case 0 => Failure(new DBException("Do Not update password info"))
      case count => Success(count)
    }
  }

  private def findUserProfile(query: Connection => PreparedStatement): Option[BasicProfile] = {
    import play.api.db._
    import play.api.Play.current

    var profile = None: Option[BasicProfile]
    DB.withConnection { conn =>
      val finalStm = query(conn)
      val res = finalStm.executeQuery
      if (res.next) {
        val oauth1Info = if (res.getLong("UserProfile.oAuth1InfoId") == 0) {
          None
        } else {
          val keys = Seq("token", "secret").map("OAuth1Info." + _)
          val values = keys.map { res.getString(_) }
           Some(new OAuth1Info(values(0), values(2)))
        }
        val oauth2Info = if (res.getLong("UserProfile.oAuth2InfoId") == 0) {
          None
        } else {
          val keys = Seq("accessToken", "tokenType", "refreshToken").map("OAuth2Info." + _)
          val values = keys.map { res.getString(_) }
          Some(new OAuth2Info(
            values(0), Some(values(1)),
            Some(res.getInt("OAuth2Info.expiresIn")), Some(values(2))
          ))
        }
        val passwordInfo = if (res.getLong("UserProfile.passwordInfoId") == 0) {
          None
        } else {
          val keys = Seq("hasher", "password", "salt").map("PasswordInfo." + _)
          val values = keys.map { res.getString(_) }
          Some(new PasswordInfo(values(0), values(1), Some(values(2))))
        }
        val keys = Seq("providerId", "userId", "firstName", "email", "avatarUrl", "authMethod").map("UserProfile." + _)
        val values = keys.map { res.getString(_) }
        profile = Some(new BasicProfile(
          values(0), values(1),
          Some(values(2)), Some(values(3)),
          Some(values(2) + " " + values(3)),
          Some(values(4)), Some(values(5)),
          new AuthenticationMethod(values(6)),
          oauth1Info, oauth2Info, passwordInfo
        ))
      }
    }
    profile
  }

  def getPasswordInfoId(conn: Connection, userId: String): Try[Int] = {
    val stm = conn.prepareStatement("""
      SELECT UserProfile.passwordInfoId FROM UserProfile
      WHERE userId=? && providerId=?
    """)
    JDBCHelper.setValue(stm, 1, userId)
    JDBCHelper.setValue(stm, 2, UsernamePasswordProvider.UsernamePassword)
    val rs = stm.executeQuery
    if (rs.next) {
      Success(rs.getInt("UserProfile.passwordInfoId"))
    } else {
      Failure(new DBException("No such passwordInfo"))
    }
  }


  def updatePasswordInfo(conn: Connection, passwordInfoId: Try[Int], entry: PasswordInfo): Try[Int] = {
    passwordInfoId match {
      case Success(id) => {
        val stm = conn.prepareStatement("""
          UPDATE TABLE PasswordInfo
          SET hasher=?,password=?,salt=?
          WHERE id=?
          """)
        JDBCHelper.setValue(stm, 1, entry.hasher)
        JDBCHelper.setValue(stm, 2, entry.password)
        JDBCHelper.setValue(stm, 3, entry.salt)
        JDBCHelper.setValue(stm, 4, id)
        Success(stm.executeUpdate)
      }
      case other => other
    }
  }


  def insertUserProfile(conn: Connection, entry: BasicProfile): Try[Int] = {
    entry match {
      case BasicProfile(providerId, userId, firstName, lastName, fullName, email, avatarUrl, authMethod, oAuth1Info, oAuth2Info, passwordInfo) => {
        val stm = conn.prepareStatement("""
          INSERT INTO UserProfile(providerId, userId, firstName, lastName, email, avatarUrl, authMethod, oAuth1InfoId, oAuth2InfoId, passwordInfoId)
          (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)
        Seq((3, firstName), (4, lastName), (5, email), (6, avatarUrl)).map { i=>
          JDBCHelper.setOptionString(stm, i._1, i._2)
        }
        Seq((1, providerId), (2, userId), (7, authMethod.method)).map { i =>
          JDBCHelper.setValue(stm, i._1, i._2)
        }
        JDBCHelper.addRelation(stm, 8, insertOAuth1Info(conn, oAuth1Info))
        JDBCHelper.addRelation(stm, 9, insertOAuth2Info(conn, oAuth2Info))
        JDBCHelper.addRelation(stm, 10, insertPassworInfo(conn, passwordInfo))
        JDBCHelper.insertRow(stm)
      }
    }
  }

  def insertOAuth1Info(conn: Connection, entry: Option[OAuth1Info]): Try[Option[Int]] = {
    entry match {
      case None => Success(None)
      case Some(OAuth1Info(token, secret)) => {
        val stm = conn.prepareStatement("""
          INSERT INTO OAuth1Info(token, secret) (?, ?)
        """)
        JDBCHelper.setValue(stm, 1, token)
        JDBCHelper.setValue(stm, 2, secret)
        JDBCHelper.insertRow(stm) match {
          case Success(id) => Success(Some(id))
          case Failure(e) => Failure(e)
        }
      }
    }
  }

  def insertOAuth2Info(conn: Connection, entry: Option[OAuth2Info]): Try[Option[Int]] = {
    entry match {
      case None => Success(None)
      case Some(OAuth2Info(accessToken, tokenType, expiresIn, refreshToken)) => {
        val stm = conn.prepareStatement("""
          INSERT INTO OAuth2Info(accessToken, tokenType, expiresIn, refreshToken)
          (?, ?, ?, ?)
        """)
        JDBCHelper.setValue(stm, 1, accessToken)
        JDBCHelper.setValue(stm, 2, tokenType)
        JDBCHelper.setValue(stm, 3, expiresIn)
        JDBCHelper.setValue(stm, 4, refreshToken)
        JDBCHelper.insertRow(stm) match {
          case Success(id) => Success(Some(id))
          case Failure(e) => Failure(e)
        }
      }
    }
  }

  def insertPassworInfo(conn: Connection, entry: Option[PasswordInfo]): Try[Option[Int]] = {
    entry match {
      case None => Success(None)
      case Some(PasswordInfo(hasher, password, salt)) => {
        val stm = conn.prepareStatement("""
          INSERT INTO PasswordInfo(hasher, password, salt)
          (?, ?, ?)
        """)
        JDBCHelper.setValue(stm, 1, hasher)
        JDBCHelper.setValue(stm, 2, password)
        JDBCHelper.setValue(stm, 3, salt)
        JDBCHelper.insertRow(stm) match {
          case Success(id) => Success(Some(id))
          case Failure(e) => Failure(e)
        }
      }
    }
  }
}

object JDBCHelper {

  def insertRow(stm: PreparedStatement): Try[Int] = {
    val count = stm.executeUpdate
    if (count == 1) {
      val keys = stm.getGeneratedKeys
      keys.next
      Success(keys.getInt(1))
    } else {
      Failure(new DBException("Can not insert Row: \n" + stm))
    }
  }

  def addRelation(stm: PreparedStatement, index: Int, entry: Try[Option[Int]]): PreparedStatement = {
    entry match {
      case Success(some) => setOptionInt(stm, index, some)
      case Failure(e) => throw e
    }
    stm
  }

  def setValue(stm: PreparedStatement, index: Int, value: Any): PreparedStatement = {
    value match {
      case value: Int => stm.setInt(index, value)
      case str: String => stm.setString(index, str)
    }
    stm
  }

  def setOptionString(stm: PreparedStatement, index: Int, value: Option[String]): PreparedStatement = {
    value match {
      case Some(str) => stm.setString(index, str)
      case None => stm.setNull(index, Types.VARCHAR)
    }
    stm
  }

  def setOptionInt(stm: PreparedStatement, index: Int, value: Option[Int]): PreparedStatement = {
    value match {
      case Some(value) => stm.setInt(index, value)
      case None => stm.setNull(index, Types.INTEGER)
    }
    stm
  }

}

class DBException (message: String) extends IllegalArgumentException
