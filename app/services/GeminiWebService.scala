package services

import Utils.ExceptionLogger
import models.AddressInfo
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class GeminiWebService @Inject()(ws: WSClient,
                                 conf: Configuration) {

  val log = Logger(this.getClass.getName)

  def getAddressInfo(name: String): Future[AddressInfo] = {
    log.warn(s"requesting Address Info for $name")
    conf.getOptional[String]("jobcoin.apiAddressesUrl")
      .map(url => {
        ws.url(s"$url/$name")
          .get()
          .flatMap(response => {
            log.warn(s"addressInfoResponse: ${response.body}")
            Future.successful(Json.fromJson[AddressInfo](Json.parse(response.body)).get)
          })
      })
      .getOrElse(Future.failed(ExceptionLogger.newException("Url Configuration Missing!")))
  }

  def transferCoins(fromAddress: String, toAddress: String, amount: Double): Future[Int] = {
    log.warn(s"requesting transfer amount $amount from $fromAddress to $toAddress")
    conf.getOptional[String]("jobcoin.apiTransactionsUrl")
      .map(ws.url(_)
        .withQueryStringParameters(("fromAddress" -> fromAddress), ("toAddress" -> toAddress), ("amount" -> amount.toString))
        .post("")
        .flatMap(response => response.status match {
          case 200 => Future.successful(200)
          case 422 => Future.failed(ExceptionLogger.newException(response.body)) // insufficient funds
          case _ => Future.failed(ExceptionLogger.newException(s"Bad Request: ${response.body}"))
        }))
      .getOrElse(Future.failed(ExceptionLogger.newException("Url Not Found!")))
  }

}
