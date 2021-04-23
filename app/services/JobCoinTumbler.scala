package services

import akka.actor._
import akka.stream.ActorMaterializer
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

    // step 1: send random amounts to up to 10 different house addresses
    // I've created a set of 10 House addresses via the UI to split the coins into
    var percentLeft = 1.0
    (1 to 10)
      .map(i => {
        val percentToMove = Random.between(0.0, 0.25)
        log.warn(s"count: $i, percentToMove: $percentToMove, percentLeft: $percentLeft")
        if ( percentLeft > 0.0 && i == 10) {
          // we've gotten to the last house address and still haven't used up all coins.
          // put the rest in here
          val distribution = Some((s"HouseAddress-10", percentLeft * tumbleRequest.amount))
          percentLeft = 0.0
          distribution
        }
        else if (percentToMove < percentLeft) {
          // as long as the current percentToMove is greater than what's remaining
          percentLeft = percentLeft - percentToMove
          Some((s"HouseAddress-0$i", percentToMove * tumbleRequest.amount))
        }
        else if (percentLeft > 0.0) {
          // the percent to move is greater or equal to the percent left, finish it off
          val distribution = Some((s"HouseAddress-0$i", percentLeft * tumbleRequest.amount))
          percentLeft = 0.0
          distribution
        }
        else {
          // we've used up all the dough, remaining addresses aren't needed
          None
        }
        // we now have a sequence of Option[(HouseAddress, percent)] where total percent = 100%
      })
      .flatten // remove Nones from the Seq
      .map(distribution => {
        // send message to actor for transfer to occur asynchronously
        transferActor ! TransferToHouse(requestId, tumbleRequest.fromAddress, distribution._1, getRandomClientAddress(tumbleRequest.toAddresses), distribution._2)
        })
  }

  def getRandomClientAddress(addresses: Seq[String]): String = {
    addresses(Random.nextInt(addresses.length))
  }
}

