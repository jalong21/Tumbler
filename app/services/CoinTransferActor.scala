package services

import akka.actor.{Actor, Props, Timers}
import net.sf.ehcache.{CacheManager, Element}
import play.api.Configuration
import play.api.cache.ehcache.EhCacheApi
import services.CoinTransferActor.{TransferToBank, TransferToClient, TransferToHouse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Random

object CoinTransferActor {
  def props(conf: Configuration,
            geminiWS: GeminiWebService) = Props(classOf[CoinTransferActor], conf, geminiWS)

  case class TransferToHouse(tumbleID: String, fromAddress: String, toAddress: String, finalClientAddress: String, amount: Double)

  case class TransferToClient(tumbleID: String, fromAddress: String, toAddress: String, amount: Double)

  case class TransferToBank(fromAddress: String, toAddress: String, amount: Double)

}

class CoinTransferActor(conf: Configuration,
                          geminiWS: GeminiWebService) extends Actor with Timers {

  val cacheManager = CacheManager.getInstance()
  cacheManager.addCacheIfAbsent("TumbleCache")
  val cache = cacheManager.getCache("TumbleCache")

  val MAX_WAIT_TIME = conf.getOptional[Int]("jobcoin.maxTumbleDurationMins")
    .getOrElse(30)

  override def receive: Receive = {
    case TransferToHouse(tumbleID: String, fromAddress: String, toAddress: String, finalClientAddress: String, amount: Double) => {

      geminiWS.transferCoins(fromAddress, toAddress, amount)
        .map(_ => {
            // choose random time between 1 and 30 mins,
            // and transfer to client address
            val timeInMins = Random.between(1, MAX_WAIT_TIME)
            val newMessage = TransferToClient(tumbleID, toAddress, finalClientAddress, amount)
            timers.startSingleTimer(newMessage, newMessage, timeInMins.minutes)
          })
    }
    case TransferToClient(tumbleID: String, fromAddress: String, toAddress: String, amount: Double) => {

      // reduce amount to send to client by 2%, we're keeping that
      geminiWS.transferCoins(fromAddress, toAddress, (amount * 98.0))
        .map(_ => {
          // transfer to clientAddress or Bank complete.
          // update cache for tumbleID
          Option(cache.get(tumbleID).getObjectValue.asInstanceOf[Double])
            .map(status => status + amount)
              .map(updatedAmount => cache.put(new Element(tumbleID, updatedAmount)))
        })
    }
    case TransferToBank(fromAddress: String, toAddress: String, amount: Double) => {
      geminiWS.transferCoins(fromAddress, toAddress, amount)
    }
  }
}
