package services

import Utils.TumbleCache
import akka.actor.{Actor, Props, Timers}
import net.sf.ehcache.{CacheManager, Element}
import play.api.{Configuration, Logger}
import services.CoinTransferActor.{TransferToClient, TransferToHouse}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Random

object CoinTransferActor {
  def props(conf: Configuration,
            geminiWS: GeminiWebService) = Props(classOf[CoinTransferActor], conf, geminiWS)

  case class TransferToHouse(tumbleID: String, fromAddress: String, toAddress: String, finalClientAddress: String, amount: Double)

  case class TransferToClient(tumbleID: String, fromAddress: String, toAddress: String, amount: Double)
}

class CoinTransferActor(conf: Configuration,
                          geminiWS: GeminiWebService) extends Actor with Timers {

  val log = Logger(this.getClass.getName)

  val MAX_WAIT_TIME = conf.getOptional[Int]("jobcoin.maxTumbleDurationSeconds")
    .getOrElse(1800)//30 mins in seconds

  override def receive: Receive = {
    case TransferToHouse(tumbleID: String, fromAddress: String, toAddress: String, finalClientAddress: String, amount: Double) => {

      geminiWS.transferCoins(fromAddress, toAddress, amount)
        .map(_ => {
            // choose random time between 1 and 30 mins,
            // and transfer to client address
            val timeInMins = Random.between(1, MAX_WAIT_TIME)
            val newMessage = TransferToClient(tumbleID, toAddress, finalClientAddress, amount)
            log.warn(s"transfer to $toAddress complete. transferring to $finalClientAddress in $timeInMins seconds")
            // UUID.randomUUID is neccessary as the key so matching messages don't cancel previous messages
            timers.startSingleTimer(UUID.randomUUID(), newMessage, timeInMins.seconds)
          })
    }
    case TransferToClient(tumbleID: String, fromAddress: String, toAddress: String, amount: Double) => {

      // reduce amount to send to client by 2%, we're keeping that
      geminiWS.transferCoins(fromAddress, toAddress, (amount * 0.98))
        .map(_ => {
          // transfer to clientAddress or Bank complete.
          // update cache for tumbleID
          Option(TumbleCache.getCache.get(tumbleID).getObjectValue.asInstanceOf[Double])
            .map(status => {
              log.warn(s"current status: $status. New status: ${status+amount}")
              status + amount
            })
              .map(updatedAmount => TumbleCache.getCache.put(new Element(tumbleID, updatedAmount)))
        })
    }
  }

  override def preStart(): Unit = {
    log.warn("actor starting")
    super.preStart()
  }

  override def postStop(): Unit = {
    log.warn("actor stopped")
    super.postStop()
  }
}
