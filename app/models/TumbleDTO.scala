package models

import play.api.libs.json.{Json, Reads}

case class TumbleDTO(amount: Double, fromAddress: String, toAddresses: Seq[String])
object TumbleDTO {
  implicit val jsonReads: Reads[TumbleDTO] = Json.reads[TumbleDTO]
}
