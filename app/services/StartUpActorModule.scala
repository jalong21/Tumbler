package services

import akka.actor.{Actor, Props, Timers}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Logger}
import services.DailySkimmer.StartTheThing

import javax.inject.Inject
import scala.concurrent.duration.DurationInt

object DailySkimmer {
  def props = Props[StartUpActor]()
  case object StartTheThing
}

/*
This class runs every 1 day.
It loops through all the house addresses and skims them down to 50 coins
every amount over 50 is transferred to a "bank" address
 */
class StartUpActor @Inject()(conf: Configuration) extends Actor with Timers {

  val log = Logger(this.getClass.getName)
  val startUp = StartTheThing

  timers.startSingleTimer(startUp, startUp, 1.minute)

  override def receive: Receive = {
    case StartTheThing => {

      log.warn("Starting the Thing!")

      timers.startSingleTimer(startUp, startUp, 1.day)
    }
  }
}

class StartUpActorModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindActor[StartUpActor]("StartUpActor")
  }
}
