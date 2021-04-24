package services

import akka.actor.{Actor, Props, Timers}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import services.DailySkimmer.SkimOffTheTop

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

object DailySkimmer {
  def props = Props[DailySkimmerActor]()
  case object SkimOffTheTop
}

/*
This class runs every 1 day.
It loops through all the house addresses and skims them down to 50 coins
every amount over 50 is transferred to a "bank" address
 */
class DailySkimmerActor @Inject()(conf: Configuration,
                                  geminiWS: GeminiWebService) extends Actor with Timers {

  val log = Logger(this.getClass.getName)
  val startSkim = SkimOffTheTop

  timers.startSingleTimer(startSkim, startSkim, 1.minute)

  override def receive: Receive = {
    case SkimOffTheTop => {

      log.warn("Start skimming!")

      //TODO: Pull out the conf.getOptionals into global Vars and check for them before running
      conf.getOptional[String]("jobcoin.houseAddresses")
        .map(addressJson => Json.fromJson[Seq[String]](Json.parse(addressJson)).get)
        .map(houseAddresses => houseAddresses
          .map(houseAddress => {
            geminiWS.getAddressInfo(houseAddress)
              .filter(address => address.balance.toDouble > 50.0)
              .map(address => {
                conf.getOptional[String]("jobcoin.personalBankAddress")
                  .map(bank => geminiWS.transferCoins(houseAddress, bank, address.balance.toDouble - 50.0))
              } )
          }))

      timers.startSingleTimer(startSkim, startSkim, 1.day)
    }
  }
}

class DailySkimmerModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindActor[DailySkimmerActor]("DailySkimmerModule")
  }
}