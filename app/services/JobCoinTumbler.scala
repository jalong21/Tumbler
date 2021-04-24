package services

import akka.actor._
import play.api.libs.json.Json
import models.TumbleDTO
import net.sf.ehcache.{CacheManager, Element}
import play.api.{Configuration, Logger}
import services.CoinTransferActor.TransferToHouse

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class JobCoinTumbler @Inject()(conf: Configuration,
                               geminiWS: GeminiWebService) {

  val log = Logger(this.getClass.getName)

  val cacheManager = CacheManager.getInstance()
  cacheManager.addCacheIfAbsent("TumbleCache")
  val cache = cacheManager.getCache("TumbleCache")

  val cacheDuration = 2.hours

  val system = ActorSystem("CoinActorSystem")
  val transferActor = system.actorOf(CoinTransferActor.props(conf, geminiWS), name = "CoinTransferActor")

  def checkForCompletion(tumbleId: String): Future[String] = {
    Option(cache.get(tumbleId).getObjectValue.asInstanceOf[Double])
      .map( percent => {
        val doubleString = String.format("%3.0f", percent * 100.0)
        Future.successful( s"TumbleID:$tumbleId - Percent Complete: $doubleString")
      })
        .getOrElse(Future.successful("TumbleId not Found!"))
  }

  def initializeTumble(tumbleRequest: TumbleDTO): Future[String] = {

    log.debug("Starting Tumble")
    // check that address has requested funds
    geminiWS.getAddressInfo(tumbleRequest.fromAddress)
      .flatMap(addressInfo => {
        addressInfo.balance.toDoubleOption
          .filter(amount => amount > tumbleRequest.amount)
          .map(_ => {
            // funds verified, create a UUID for transaction,
            // fire off Future to process transaction,
            // put transaction in cache for future reference.
            val requestId = UUID.randomUUID().toString
            cache.put(new Element(requestId, 0.0))
            Future { tumble(tumbleRequest, requestId)}
            // return request ID for client's future use.
            Future.successful(requestId)
          }).getOrElse( Future.failed(ExceptionLogger.newException("Error Processing Request. This Could Mean the From Address Had Insufficient Funds.")))
      })
  }

  def tumble(tumbleRequest: TumbleDTO, requestId: String) = {

    // send random amounts to up to 10 different house addresses
    var percentLeft = 1.0
    conf.getOptional[String]("jobcoin.houseAddresses")
      .map(addressJson => Json.fromJson[Seq[String]](Json.parse(addressJson)).get)
      .map(houseAddresses => houseAddresses
        .map(houseAddress => (houseAddress, Random.between(0.10, 0.25)))
      .map(distribution => {
        if(distribution._2 < percentLeft) {
          percentLeft = percentLeft - distribution._2
          transferActor ! TransferToHouse(requestId, tumbleRequest.fromAddress, distribution._1, getRandomClientAddress(tumbleRequest.toAddresses), tumbleRequest.amount * distribution._2)
        }
        else if (percentLeft > 0) {
          transferActor ! TransferToHouse(requestId, tumbleRequest.fromAddress, distribution._1, getRandomClientAddress(tumbleRequest.toAddresses), tumbleRequest.amount * percentLeft)
          percentLeft = -1
        }
        }))
  }

  def getRandomClientAddress(addresses: Seq[String]): String = {
    addresses(Random.nextInt(addresses.length))
  }
}

