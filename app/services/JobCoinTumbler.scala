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

class JobCoinTumbler @Inject()(conf: Configuration,
                               cache: EhCacheApi) {

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


    // step 2. choose 10 random amounts from remaining

    // step 3. send 10 amounts to 10 house wallets

    // step 1. take 2%

    // step 4. wait 10 random amounts of time

    // step 5. send same amounts from same house accounts to list of too accounts

    // Step 6. set cache for request Id to true so client can know it was done


  }
}

