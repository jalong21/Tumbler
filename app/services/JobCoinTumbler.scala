package services

import akka.actor.{ActorSystem, Props}
import models.TumbleDTO
import play.api.cache.ehcache.EhCacheApi

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class JobCoinTumbler @Inject()(cache: EhCacheApi,
                               geminiWS: GeminiWebService) {

  val cacheDuration = 2.hours

  val system = ActorSystem("CoinActorSystem")
  val transferActor = system.actorOf(Props[CoinTransferActor](), name = "CoinTransferActor")

  def checkForCompletion(tumbleId: String) = cache.get[Double](tumbleId)
      .flatMap(cachedValue => cachedValue
        .map( percent => Future.successful( s"TumbleID:$tumbleId - Percent Complete: $percent"))
      .getOrElse(Future.successful("TumbleId not Found!")))

  def initializeTumble(tumbleRequest: TumbleDTO): Future[String] = {

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
            cache.set(requestId, 0.0, cacheDuration)
            Future { tumble(tumbleRequest, requestId)}
            // return request ID for client's future use.
            Future.successful(requestId)
          }).getOrElse( Future.failed(new Exception("Error Processing Request. This Could Mean the From Address Had Insufficient Funds.")))
      })
  }

  def tumble(tumbleRequest: TumbleDTO, requestId: String) = {

    // step 1: send random amounts to up to 10 different house addresses
    // I've created a set of 10 House addresses via the UI to split the coins into
    var percentLeft = 100.0
    (1 to 10)
      .map(i => {
        val percentToMove = Random.between(0.0, 25.0)
        if ( percentLeft > 0 && i == 10) {
          // we've gotten to the last house address and still haven't used up all coins.
          // put the rest in here
          percentLeft = 0
          Some((s"HouseAddress-10", percentLeft * tumbleRequest.amount))
        }
        else if (percentToMove < percentLeft) {
          // as long as the current percentToMove is greater than what's remaining
          percentLeft = percentLeft - percentToMove
          Some((s"HouseAddress-0$i", percentToMove * tumbleRequest.amount))
        }
        else if (percentLeft > 0) {
          // the percent to move is greater or equal to the percent left, finish it off
          percentLeft = 0
          Some((s"HouseAddress-0$i", percentLeft * tumbleRequest.amount))
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

  def getRandomClientAddress(addresses: Seq[String]) = {
    addresses(Random.nextInt(addresses.length))
  }
}

