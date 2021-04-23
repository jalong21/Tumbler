package services

import models.AddressInfo
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import javax.inject.Inject
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class GeminiWebService @Inject()(ws: WSClient,
                                 conf: Configuration) {

  def getAddressInfo(name: String) = {
    conf.getOptional[String]("jobcoin.apiAddressesUrl")
      .map(url => ws.url(s"url/$name")
        .get()
        .map(response => {
          Json.fromJson[AddressInfo](Json.parse(response.body)).get
        }))
      .getOrElse(Future.failed(new Exception("Url Configuration Missing!")))
  }

  def transferCoins(fromAddress: String, toAddress: String, amount: Double): Future[Int] = {
    conf.getOptional[String]("jobcoin.apiAddressesUrl")
      .map(ws.url(_)
        .withQueryStringParameters(("fromAddress" -> fromAddress), ("toAddress" -> toAddress), ("amount" -> amount.toString))
        .post("")
        .flatMap(response => response.status match {
          case 200 => Future.successful(200)
          case 422 => Future.failed(new Exception(response.body)) // insufficient funds
          case _ => Future.failed(new Exception("Bad Request"))
        }))
      .getOrElse(Future.failed(new Exception("Url Not Found!")))
  }

}
