// See the LICENCE file distributed with this work for licence info.
import java.lang.reflect.Constructor
import play.api.inject._
import play.api.{ Configuration, Environment }
import securesocial.core.providers.utils.{ Mailer, PasswordHasher, PasswordValidator }
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers._
import securesocial.core.services.{ UserService }
import services.{DatabaseUserService, UserEnvironment, ImplementedUserEnvironment}
import models.{User}

class AnimalModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[UserEnvironment].to[ImplementedUserEnvironment],
      bind[RuntimeEnvironment].to[ImplementedUserEnvironment],
      bind[UserService[User]].to[DatabaseUserService]
      //    bind[User].to[User]
    )
  }
}
