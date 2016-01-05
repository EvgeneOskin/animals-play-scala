import java.lang.reflect.Constructor
import play.api.inject._
import play.api.{ Configuration, Environment }
import securesocial.core.providers.utils.{ Mailer, PasswordHasher, PasswordValidator }
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers._
import securesocial.core.services.{ UserService }
import scala.collection.immutable.ListMap
import services.{DatabaseUserService}
import models.{User}

/**
 * Runtime environment
 */
object DemoRuntimeEnvironment extends RuntimeEnvironment.Default {
  type U = User
  def userService: UserService[U] =
    new DatabaseUserService

  override lazy val providers = ListMap(
    include(new UsernamePasswordProvider[User](userService, avatarService, viewTemplates, passwordHashers))
  )
}



class AnimalModule extends Module {
  def bindings(environment: Environment,
    configuration: Configuration) = Seq(
      bind[RuntimeEnvironment].toInstance(DemoRuntimeEnvironment)
      //    bind[DatabaseUserService].to[DatabaseUserService],
      //    bind[User].to[User]
    )
}
