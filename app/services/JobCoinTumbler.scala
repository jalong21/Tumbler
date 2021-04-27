package services

import Utils.{ExceptionLogger, TumbleCache}
import akka.actor._
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import play.api.libs.json.Json
import models.{TumbleDTO, TumbleInitialized, TumbleStatus}
import net.sf.ehcache.{CacheManager, Element}
import play.api.Play.materializer
import play.api.{Configuration, Logger}
import services.CoinTransferActor.TransferToHouse

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class JobCoinTumbler @Inject()(conf: Configuration,
                               geminiWS: GeminiWebService,
                               implicit val materializer: Materializer) {

  val log = Logger(this.getClass.getName)

  val cacheDuration = 2.hours

  val system = ActorSystem("CoinActorSystem")
  val transferActor = system.actorOf(CoinTransferActor.props(conf, geminiWS), name = "CoinTransferActor")

  val HOUSE_ADDRESSES = conf.getOptional[String]("jobcoin.houseAddresses")
    .map(addressJson => Json.fromJson[Seq[String]](Json.parse(addressJson)).get)
    .get

  def checkForCompletion(tumbleId: String): Future[TumbleStatus] = {
    Option(TumbleCache.getCache.get(tumbleId).getObjectValue.asInstanceOf[Double])
      .map( percent => Future.successful( TumbleStatus(tumbleId, percent) ))
        .getOrElse(Future.failed(ExceptionLogger.newException("TumbleId not Found!")))
  }

  def initializeTumble(tumbleRequest: TumbleDTO): Future[TumbleInitialized] = {

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
            TumbleCache.getCache.put(new Element(requestId, 0.0))
            Future { tumble(tumbleRequest, requestId) }
            // return request ID for client's future use.
            Future.successful( TumbleInitialized(requestId))
          }).getOrElse( Future.failed(ExceptionLogger.newException("Error Processing Request. This Could Mean the From Address Had Insufficient Funds.")))
      })
  }

  def tumble(tumbleRequest: TumbleDTO, requestId: String) = {

    // send random amounts to random house addresses until 100% has been transferred
    var percentLeft = 1.0
    def takePercent(): Future[Double] = {
      // assign a random amount between 0.01 and 0.10 to a random selection from house addresses
      val percent = Random.between(0.01, 0.10)
      val address = getRandomAddress(HOUSE_ADDRESSES)
      if(percent < percentLeft) {
        percentLeft = percentLeft - percent
        transferActor ! TransferToHouse(requestId, tumbleRequest.fromAddress, address, getRandomAddress(tumbleRequest.toAddresses), tumbleRequest.amount * percent)
        Future.successful(percent)
      }
      else {
        transferActor ! TransferToHouse(requestId, tumbleRequest.fromAddress, address, getRandomAddress(tumbleRequest.toAddresses), tumbleRequest.amount * percentLeft)
        Future.successful(-1.0)
      }
    }

    // Take while percentLeft > 0, using takePercent method
    Source(1 to 100)
      .mapAsync(1)(_ => takePercent())
      .takeWhile(result => result > 0)
      .runWith(Sink.last[Double])
  }

  def getRandomAddress(addresses: Seq[String]): String = {
    addresses(Random.nextInt(addresses.length))
  }
}

