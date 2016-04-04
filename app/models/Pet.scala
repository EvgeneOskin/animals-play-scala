// See the LICENCE file distributed with this work for licence info.
package models

import java.math.BigDecimal
import java.sql.{Types, Connection, Statement, PreparedStatement}
import scala.util.{Try, Success, Failure}
import play.api.Logger

import models.User

case class Breed(
  name: String
)

case class Pet(
  breed: Breed,
  owner: User,
  gender: String,
  name: String
)

case class Birth(
  child: Pet,
  father: Pet,
  mother: Pet
)

object Pet {
}

class DBException (message: String) extends IllegalArgumentException
