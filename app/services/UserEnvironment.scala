package services
import java.lang.reflect.Constructor
import play.api.inject._
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers._
import securesocial.core.services.{ UserService }
import scala.collection.immutable.ListMap
import models.{User}

class UserEnvironment extends RuntimeEnvironment.Default {
  type U = User

  def userService: UserService[U] = {
    NewInstanceInjector.instanceOf[UserService[U]]
  }

  override lazy val providers = ListMap(
    include(new UsernamePasswordProvider[U](
      userService, avatarService, viewTemplates, passwordHashers
    ))
  )
}


