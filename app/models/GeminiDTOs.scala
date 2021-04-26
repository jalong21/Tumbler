package models

import play.api.libs.json.{Json, Reads}

case class AddressInfo(balance: String)
object AddressInfo {
  implicit val jsonReads: Reads[AddressInfo] = Json.reads[AddressInfo]
}


