// See the LICENCE file distributed with this work for licence info.
package controllers

import javax.inject._
import play.api.mvc.{ Action, RequestHeader, Controller }
import securesocial.core._
import models.User

class Application @Inject() (
  override implicit val env: RuntimeEnvironment
) extends Controller with securesocial.core.SecureSocial {

  def index = SecuredAction { implicit request =>
    Ok(views.html.index("Your new application is ready."))
  }
}
