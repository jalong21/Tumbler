package controllers

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
                case Success(tumbleId) => Ok(Json.toJson(tumbleId))
                case Failure(ex) => InternalServerError(s"Error Starting Transfer. ${ex.getMessage}")
              }
            }
            case Failure(ex) => BadRequest(s"Error Parsing Request Body! ${ex.getMessage}")
          }
        }).getOrElse(BadRequest("Payload Missing From Request"))
  }

  def isTumbleComplete(tumbleId: String) = Action {
    request => Try(Await.result(tumbler.checkForCompletion(tumbleId), 5.minutes)) match {
      case Success(status) => Ok(Json.toJson(status))
      case Failure(ex) => InternalServerError(s"Error Checking Status.  ${ex.getMessage}")
    }
  }
}