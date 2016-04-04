package service

import org.joda.time.DateTime
import scala.concurrent.Future


object SaveMode extends Enumeration {
  val create = Value("create")
  val update = Value("update")
}

trait ModelService[T] {
  def find(id: String): Future[Option[T]]
  def list(): Future[List[T]]
  def save(instance: T, mode: SaveMode): Future[T]
}
