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

}
