package models

import play.api.libs.json.{Json, Reads, Writes}

case class TumbleDTO(amount: Double, fromAddress: String, toAddresses: Seq[String])
object TumbleDTO {
  implicit val jsonReads: Reads[TumbleDTO] = Json.reads[TumbleDTO]
}

case class TumbleInitialized(uuid: String)
object TumbleInitialized {
  implicit val jsonWrites: Writes[TumbleInitialized] = Json.writes[TumbleInitialized]
}

case class TumbleStatus(uuid: String, percentComplete: Double)
object TumbleStatus {
  implicit val jsonWrites: Writes[TumbleStatus] = Json.writes[TumbleStatus]
}