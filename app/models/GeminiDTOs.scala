package models

import play.api.libs.json.{Json, Reads}

case class AddressInfo(balance: String)
object AddressInfo {
  implicit val jsonReads: Reads[AddressInfo] = Json.reads[AddressInfo]
}

case class Transactions(bye: String)
object Transactions {
  implicit val jsonReads: Reads[Transactions] = Json.reads[Transactions]
}


