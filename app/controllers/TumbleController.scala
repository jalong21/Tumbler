package controllers

import akka.actor.ActorSystem
import models.TumbleDTO
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import services.JobCoinTumbler

import javax.inject.Inject
import scala.concurrent.Await
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

class TumbleController @Inject()(cc: ControllerComponents,
                                 tumbler: JobCoinTumbler) extends AbstractController(cc) {

  val log = Logger(this.getClass.getName)

  def initializeTumble() = Action {
    request => request.body.asJson
        .map( requestBody => {
          log.warn(s"requestBody: ${requestBody.toString()}")
          Try(Json.fromJson[TumbleDTO](requestBody).get) match {
            case Success(tumbleRequest) => {
              log.warn("calling tumbler")
              Try(Await.result(tumbler.initializeTumble(tumbleRequest), 5.minutes)) match {
                case Success(tumbleId) => Ok(tumbleId)
                case Failure(_) => InternalServerError("Error Starting Transfer")
              }
            }
            case Failure(_) => BadRequest("Error Parsing Request Body!")
          }
        }).getOrElse(BadRequest("Payload Missing From Request"))
  }

  def isTumbleComplete(tumbleId: String) = Action {
    request => Try(Await.result(tumbler.checkForCompletion(tumbleId), 5.minutes)) match {
      case Success(status) => Ok(status)
      case Failure(_) => InternalServerError("Error Checking Status")
    }
  }
}