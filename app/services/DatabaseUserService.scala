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
    if (logger.isDebugEnabled) {
      logger.debug("users = %s".format(users))
    }
    Future.successful(User.find(providerId, userId))
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    if (logger.isDebugEnabled) {
      logger.debug("users = %s".format(users))
    }
    Future.successful(User.findByEmailAndProvider(email, providerId))
  }

  private def findProfile(p: BasicProfile) = {
    User.find(p.providerId, p.userId)
  }

  def save(user: BasicProfile, mode: SaveMode): Future[User] = {
    mode match {
      case SaveMode.SignUp =>
        val newUser = User(user, List(user))
        users = users + ((user.providerId, user.userId) -> newUser)
        Future.successful(newUser)
      case SaveMode.LoggedIn =>
        // first see if there is a user with this BasicProfile already.
        findProfile(user) match {
          case Some(existingUser) =>
            updateProfile(user, existingUser)

          case None =>
            val newUser = User(user, List(user))
            users = users + ((user.providerId, user.userId) -> newUser)
            Future.successful(newUser)
        }

      case SaveMode.PasswordChange =>
        findProfile(user).map { entry => updateProfile(user, entry) }.getOrElse(
          // this should not happen as the profile will be there
          throw new Exception("missing profile)")
        )
    }
  }

  def link(current: User, to: BasicProfile): Future[User] = {
    if (current.identities.exists(i => i.providerId == to.providerId && i.userId == to.userId)) {
      Future.successful(current)
    } else {
      val added = to :: current.identities
      val updatedUser = current.copy(identities = added)
      users = users + ((current.main.providerId, current.main.userId) -> updatedUser)
      Future.successful(updatedUser)
    }
  }

  def saveToken(token: MailToken): Future[MailToken] = {
    Future.successful {
      cache.set(token.uuid, token, (token.expirationTime.getMillis - (new DateTime).getMillis) millis)
      token
    }
  }

  def findToken(token: String): Future[Option[MailToken]] = {
    Future.successful { cache.get[MailToken](token) }
  }

  def deleteToken(uuid: String): Future[Option[MailToken]] = {
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
        found <- users.values.find(_ == user);
        identityWithPasswordInfo <- found.identities.find(_.providerId == UsernamePasswordProvider.UsernamePassword)
      ) yield {
        val idx = found.identities.indexOf(identityWithPasswordInfo)
        val updated = identityWithPasswordInfo.copy(passwordInfo = Some(info))
        val updatedIdentities = found.identities.patch(idx, Seq(updated), 1)
        val updatedEntry = found.copy(identities = updatedIdentities)
        users = users + ((updatedEntry.main.providerId, updatedEntry.main.userId) -> updatedEntry)
        updated
      }
    }
  }

  override def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = {
    Future.successful {
      for (
        found <- users.values.find(u => u.main.providerId == user.main.providerId && u.main.userId == user.main.userId);
        identityWithPasswordInfo <- found.identities.find(_.providerId == UsernamePasswordProvider.UsernamePassword)
      ) yield {
        identityWithPasswordInfo.passwordInfo.get
      }
    }
  }
}
