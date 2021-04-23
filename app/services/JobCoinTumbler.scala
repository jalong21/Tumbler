package services

import models.TumbleRequest
import play.api.Configuration
import play.api.cache.ehcache.EhCacheApi
import play.api.libs.ws.WSClient

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class JobCoinTumbler @Inject()(conf: Configuration,
                               cache: EhCacheApi,
                               geminiWS: GeminiWebService) {

  val cacheDuration = 2.hours

  def checkForCompletion(tumbleId: String) = {
    cache.get[Boolean](tumbleId)
  }

  def initializeTumble(tumbleRequest: TumbleRequest): String = {

    val requestId = UUID.randomUUID().toString
    cache.set(requestId, false, cacheDuration)
    Future { tumble(tumbleRequest, requestId)}
    requestId
  }

  def tumble(tumbleRequest: TumbleRequest, requestId: String) = {

    // step 1: send random amounts to up to 10 different house addresses
    var percentLeft = 100.0
    val houseAmountTuple: Seq[(String, Double)] = (1 to 10)
      .map(i => {
        val percentToMove = Random.between(0.0, 20.0)
        if ( i == 10) {
          // we've gotten to the last house address and still haven't used up all coins.
          // put the rest in here
          percentLeft = 0
          Some((s"HouseAddress-10", percentLeft * tumbleRequest.amount))
        }
        else if (percentToMove < percentLeft) {
          // as long as the current percentage is greater than what's remaining
          percentLeft = percentLeft - percentToMove
          Some((s"HouseAddress-0$i", percentToMove * tumbleRequest.amount))
        }
        else if (percentLeft > 0) {
          // the percent to move is greater or equal to the percent left, finish it off
          percentLeft = 0
          Some((s"HouseAddress-0$i", percentLeft * tumbleRequest.amount))
        }
        else {
          None
        }
        // we now have a sequence of Option[(HouseAddress, percent)] where percents = 100%
      })
      .flatten
      .map(distribution => {
          geminiWS.transferCoins(tumbleRequest.fromAddress, distribution._1, distribution._2.toString)
        // reduce the amount to move by 2% which will stay in the house address as payment
        // TODO: consider creating a daily job that skims house addresses down to certain levels and depsosits in personal address
        (distribution._1, distribution._2 * 0.98)
        })

        // next step: Use actors and timers to choose random time over next 30 mins
        // to move amount from house addresses to user's too addresses

  }
}

