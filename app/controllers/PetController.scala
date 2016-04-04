// See the LICENCE file distributed with this work for licence info.
package controllers

import javax.inject._
import play.api.mvc.{ Action, RequestHeader, Controller }
import models.{ Pet }
import services.UserEnvironment
import securesocial.core._

class PetController @Inject() (
  implicit val service: ModelService[Pet]
) extends Controller {

  def index = UserAwareAction { implicit request =>
    val userName = request.user match {
      case Some(user) => user.main.fullName.getOrElse {"guest"}
      case _ => "guest"
    }
    val pets = service.list()
    Ok(views.html.pet.index(pets)(userName))
  }

  def create = UserAwareAction { implicit request =>
    val pet = service.create()
    Ok(Json.toJson(pet))
  }

  def update(id: String) = UserAwareAction {
  }

  def get(id: String) = UserAwareAction {
    val userName = request.user match {
      case Some(user) => user.main.fullName.getOrElse {"guest"}
      case _ => "guest"
    }
    val pet: Pet = service.get(id)
    Ok(views.html.pet.show(pets)(userName))
  }
}
