package models

import play.api.libs.json.{Json, Reads}

case class TumbleRequest(amount: Double, fromAddress: String, tooAddresses: Seq[String])
object TumbleRequest {
  implicit val jsonReads: Reads[TumbleRequest] = Json.reads[TumbleRequest]
}
