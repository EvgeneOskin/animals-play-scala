// See the LICENCE file distributed with this work for licence info.
package services

import org.joda.time.DateTime
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Logger
import play.api.cache._
import securesocial.core._
import securesocial.core.providers.{ UsernamePasswordProvider, MailToken }
import securesocial.core.services.{ UserService, SaveMode }
import models.{ User }

/**
 * A Sample In database user service in Scala
 *
 */
class DatabaseUserService @Inject() (
    cache: CacheApi
) extends UserService[User] {
  val logger = Logger("application.controllers.DatabaseUserService")

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    logger.debug("find user for providerId=%s useId=%s".format(providerId, userId))
    Future.successful(User.find(providerId, userId))
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    logger.debug("find user for providerId=%s email=%s".format(providerId, email))
    Future.successful(User.findByEmailAndProvider(email, providerId))
  }

  def save(user: BasicProfile, mode: SaveMode): Future[User] = {
    logger.debug("Save user with providerId=%s email=%s mode=%s".format(user.providerId, user.email, mode))
    mode match {
      case SaveMode.SignUp =>
        Future.successful(createNewProfile(user))

      case SaveMode.LoggedIn =>
        // first see if there is a user with this BasicProfile already.
        findProfile(user) match {
          case Some(existingProfile) =>
            updateProfile(user, existingProfile)

          case None =>
            Future.successful(createNewProfile(user))
        }

      case SaveMode.PasswordChange =>
        findProfile(user).map { entry => updateProfile(user, entry) }.getOrElse(
          // this should not happen as the profile will be there
          throw new Exception("missing profile)")
        )
    }
  }

  def link(current: User, to: BasicProfile): Future[User] = {
    logger.debug("Link profile with providerId=%s to user with userId=%s".format(to.providerId, current.main.userId))
    User.find(to.providerId, current.main.userId) match {
      case Some(_) => Future.successful(current)
      case None => {
        val userProfile = new BasicProfile(
          to.providerId, current.main.userId,
          to.firstName, to.lastName,
          to.fullName, to.email,
          to.avatarUrl, to.authMethod,
          to.oAuth1Info, to.oAuth2Info,
          to.passwordInfo
        )
        User.save(userProfile)
        val added = to :: current.identities
        val updatedUser = current.copy(identities = added)
        Future.successful(updatedUser)
      }
    }
  }

  def saveToken(token: MailToken): Future[MailToken] = {
    logger.debug("Save mail token")
    Future.successful {
      cache.set(token.uuid, token, (token.expirationTime.getMillis - (new DateTime).getMillis) millis)
      token
    }
  }

  def findToken(token: String): Future[Option[MailToken]] = {
    logger.debug("Find mail token")
    Future.successful { cache.get[MailToken](token) }
  }

  def deleteToken(uuid: String): Future[Option[MailToken]] = {
    logger.debug("Delete mail token")
    Future.successful {
      val token =cache.get[MailToken](uuid)
      cache.remove(uuid)
      token
    }
  }

  def deleteExpiredTokens() {
    // Setting Cache with duration provide cleaning.
  }

  override def updatePasswordInfo(user: User, info: PasswordInfo): Future[Option[BasicProfile]] = {
    Future.successful {
      for (
        identityWithPasswordInfo <- findPasswordProfiles(user.main)
      ) yield {
        val updated = identityWithPasswordInfo.copy(passwordInfo = Some(info))
        User.updatePasswordInfo(user.main, info)
        updated
      }
    }
  }

  override def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = {
    Future.successful {
      for (
        identityWithPasswordInfo <- findPasswordProfiles(user.main)
      ) yield {
        identityWithPasswordInfo.passwordInfo.get
      }
    }
  }

  private def findProfile(p: BasicProfile): Option[BasicProfile] = {
    User.find(p.providerId, p.userId)
  }

  private def findPasswordProfiles(p: BasicProfile): Option[BasicProfile] = {
    User.find(UsernamePasswordProvider.UsernamePassword, p.userId)
  }

  private def createNewProfile(user: BasicProfile): User = {
    User.save(user)
    new User(user, List(user))
  }

  private def updateProfile(user: BasicProfile, existedUser:BasicProfile): Future[User] = {
    Future.successful(new User(user, List(user)))
  }
}
