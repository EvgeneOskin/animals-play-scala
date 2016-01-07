package models

import java.math.BigDecimal
import java.sql.{Types, Connection, PreparedStatement}
import scala.util.{Try, Success, Failure}
import securesocial.core._

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

  private def findUserProfile(query: Connection => PreparedStatement): Option[BasicProfile] = {
    import play.api.db._
    import play.api.Play.current

    var profile = None: Option[BasicProfile]
    DB.withConnection { conn =>
      val finalStm = query(conn)
      val res = finalStm.executeQuery
      if (res.next()) {
        val oauth1Info = if (res.getLong("UserProfile.oAuth1InfoId") == 0) None
        else
           Some(new OAuth1Info(
            res.getString("OAuth1Info.token"),
            res.getString("OAuth1Info.secret")
          ))
        val oauth2Info = if (res.getLong("UserProfile.oAuth2InfoId") == 0) None
        else
          Some(new OAuth2Info(
            res.getString("OAuth2Info.accessToken"),
            Some(res.getString("OAuth2Info.tokenType")),
            Some(res.getInt("OAuth2Info.expiresIn")),
            Some(res.getString("OAuth2Info.refreshToken"))
          ))
        val passwordInfo = if (res.getLong("UserProfile.passwordInfoId") == 0) None
        else
          Some(new PasswordInfo(
            res.getString("PasswordInfo.hasher"),
            res.getString("PasswordInfo.password"),
            Some(res.getString("PasswordInfo.salt"))
          ))

        profile = Some(new BasicProfile(
          res.getString("UserProfile.providerId"),
          res.getString("UserProfile.userId"),
          Some(res.getString("UserProfile.firstName")),
          Some(res.getString("UserProfile.lastName")),
          Some(res.getString("UserProfile.firstName")  + " " + res.getString("UserProfile.lastName")),
          Some(res.getString("UserProfile.email")),
          Some(res.getString("UserProfile.avatarUrl")),
          new AuthenticationMethod(res.getString("UserProfile.authMethod")),
          oauth1Info,
          oauth2Info,
          passwordInfo
        ))
      }
    }
    profile
  }

  def find(providerId: String, userId: String): Option[BasicProfile] = {
    findUserProfile { conn =>
      val query = s"""
        $baseQuery
        where UserProfile.providerId = ? && UserProfile.userId = ?
        limit 1
      """
      val prep = conn.prepareStatement(query)
      prep.setString(1, providerId)
      prep.setString(2, userId)
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
      prep.setString(1, providerId)
      prep.setString(2, email)
      prep
    }
  }

  def insertRow(stm: PreparedStatement): Try[Option[Int]] = {
    val count = stm.executeUpdate
    if (count == 1) {
      val keys = stm.getGeneratedKeys
      keys.next
      Success(Some(keys.getInt(1)))
    } else {
      Failure(new DBException("Can not insert Row: \n" + stm))
    }
  }

  def insertInfo(conn: Connection, entry: Option[OAuth1Info]): Try[Option[Int]] = {
    entry match {
      case None => Success(None)
      case Some(OAuth1Info(token, secret)) => {
        val stm = conn.prepareStatement("""
          INSERT INTO OAuth1Info(token, secret) (?, ?)
        """)
        stm.setString(1, token)
        stm.setString(2, secret)
        insertRow(stm)
      }
    }
  }

  def insertInfo(conn: Connection, entry: Option[OAuth2Info]): Try[Option[Int]] = {
    entry match {
      case None => Success(None)
      case Some(OAuth2Info(accessToken, tokenType, expiresIn, refreshToken)) => {
        val stm = conn.prepareStatement("""
          INSERT INTO OAuth2Info(accessToken, tokenType, expiresIn, refreshToken)
          (?, ?, ?, ?)
        """)
        stm.setString(1, accessToken)
        setValue(stm, 2, tokenType)
        setValue(stm, 3, expiresIn)
        setValue(stm, 4, refreshToken)
        insertRow(stm)
      }
    }
  }

  def insertInfo(conn: Connection, entry: Option[PasswordInfo]): Try[Option[Int]] = {
    entry match {
      case None => Success(None)
      case Some(PasswordInfo(hasher, password, salt)) => {
        val stm = conn.prepareStatement("""
          INSERT INTO PasswordInfo(hasher, password, salt)
          (?, ?, ?)
        """)
        stm.setString(1, hasher)
        stm.setString(2, password)
        setValue(stm, 3, salt)
        insertRow(stm)
      }
    }
  }

  def insertUserProfile(conn: Connection, entry: BasicProfile): Try[Option[Int]] = {
    entry match {
      case BasicProfile(providerId, userId, firstName, lastName, fullName, email, avatarUrl, authMethod, oAuth1Info, oAuth2Info, passwordInfo) => {
        val stm = conn.prepareStatement("""
          INSERT INTO UserProfile(providerId, userId, firstName, lastName, email, avatarUrl, authMethod, oAuth1InfoId, oAuth2InfoId, passwordInfoId)
          (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)
        Seq((3, firstName), (4, lastName), (5, email), (6, avatarUrl)).map { i=>
          setValue(stm, i._1, i._2)
        }
        Seq((1, providerId), (2, userId), (7, authMethod.method)).map { i =>
          setValue(stm, i._1, i._2)
        }
        addRelation(stm, 8, insertInfo(conn, oAuth1Info))
        addRelation(stm, 9, insertInfo(conn, oAuth2Info))
        addRelation(stm, 10, insertInfo(conn, passwordInfo))
        insertRow(stm)
      }
    }
  }

  def addRelation(stm: PreparedStatement, index: Int, entry: Try[Option[Int]]): PreparedStatement = {
    entry match {
      case Success(Some(id: Int)) => stm.setBigDecimal(index, new BigDecimal(id))
      case Success(None) => stm.setNull(index, Types.BIGINT)
      case Failure(e) => throw e
    }
    stm
  }

  def setValue(stm: PreparedStatement, index: Int, str: String): PreparedStatement = {
    stm.setString(index, str)
    stm
  }

  def setValue(stm: PreparedStatement, index: Int, someStr: Option[String]): PreparedStatement = {
    someStr match {
      case None => stm.setNull(index, Types.VARCHAR)
      case Some(str) => stm.setString(index, str)
    }
    stm
  }

  def setValue(stm: PreparedStatement, index: Int, someInt: Option[Int]): PreparedStatement = {
    someInt match {
      case None => stm.setNull(index, Types.INTEGER)
      case Some(value) => stm.setInt(index, value)
    }
    stm
  }

  def save(entry: BasicProfile): BasicProfile = {
    import play.api.db._
    import play.api.Play.current

    var insertedId: String = ""
    DB.withConnection { conn =>
      insertUserProfile(conn, entry) match {
        case Success(Some(id: Int)) => insertedId = id.toString
        case Failure(e) => throw e
      }
    }
    entry
  }
}

class DBException (message: String) extends IllegalArgumentException
