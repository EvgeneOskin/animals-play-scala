// See the LICENCE file distributed with this work for licence info.
package controllers

import javax.inject._
import play.api.mvc.{ Action, RequestHeader, Controller }
import models.{ Pet }
import services.UserEnvironment
import securesocial.core._

class PetApiController @Inject() (
  override implicit val service: ModelService[Pet]
) extends Controller {

  def list = UserAwareAction { implicit request =>
    val userName = request.user match {
      case Some(user) => user.main.fullName.getOrElse {"guest"}
      case _ => "guest"
    }
    Ok("OK")
  }

  def create = UserAwareAction { implicit request =>
    Ok("OK")
  }

  def update(id: String) = UserAwareAction {
    Ok("OK")
  }

  def retrieve(id: String) = UserAwareAction {
    Ok("OK")
  }
}
