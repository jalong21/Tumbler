package services

import akka.actor.{Actor, Timers}
import play.api.Configuration
import play.api.cache.ehcache.EhCacheApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Random

case class TransferToHouse(tumbleID: String, fromAddress: String, toAddress: String, finalClientAddress: String, amount: Double)

case class TransferToClient(tumbleID: String, fromAddress: String, toAddress: String, amount: Double)

case class TransferToBank(fromAddress: String, toAddress: String, amount: Double)


class CoinTransferActor()(conf: Configuration,
                          geminiWS: GeminiWebService,
                          cache: EhCacheApi) extends Actor with Timers {

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
          cache.get[Double](tumbleID)
            .map(cacheStatus => cacheStatus
              .map(status => status + amount)
              .map(updatedAmount => cache.set(tumbleID, updatedAmount, 2.hours)))
        })
    }
    case TransferToBank(fromAddress: String, toAddress: String, amount: Double) => {
      geminiWS.transferCoins(fromAddress, toAddress, amount)
    }
  }
}
