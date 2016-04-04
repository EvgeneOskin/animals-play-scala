// See the LICENCE file distributed with this work for licence info.
package services

import org.joda.time.DateTime
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Logger
import play.api.cache._
import models.{ Pet, User }


class DatabasePetService @Inject() extends ModelService[Pet] {
  val logger = Logger("application.services.PetService")

  def find(id: String): Future[Option[Pet]] = {
    logger.debug("find pet for id=%s".format(id))
    Future.successful(Pet.find(id))
  }

  def list(): Future[List[Pet]] = {
    logger.debug("List pets")
    Future.successful(Pet.list())
  }

  def save(intance: Pet, mode: SaveMode): Future[Pet] = {
    Future.successful(mode match {
      case SaveMode.update => {
        Pet.update(instance.id, instance)
      }
      case SaveMode.create => {
        Pet.create(instance)
      }
    })
  }
}
