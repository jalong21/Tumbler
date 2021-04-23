package controllers

import models.TumbleDTO
import play.api.libs.json.Json
import play.api.mvc._
import services.JobCoinTumbler

import javax.inject.Inject
import scala.concurrent.Await
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

class TumbleController @Inject()(cc: ControllerComponents,
                                 tumbler: JobCoinTumbler) extends AbstractController(cc) {

  def initializeTumble() = Action {
    request => request.body.asJson
        .map( requestBody => Try(Json.fromJson[TumbleDTO](requestBody).get) match {
          case Success(tumbleRequest) => {
            Try(Await.result(tumbler.initializeTumble(tumbleRequest), 5.seconds)) match {
              case Success(tumbleId) => Ok(tumbleId)
              case Failure(_) => InternalServerError("Error Starting Transfer")
            }
          }
          case Failure(_) => BadRequest("Error Parsing Request Body!")
        }).getOrElse(BadRequest("Payload Missing From Request"))
  }

  def isTumbleComplete(tumbleId: String) = Action {
    request => Try(Await.result(tumbler.checkForCompletion(tumbleId), 5.seconds)) match {
      case Success(status) => Ok(status)
      case Failure(_) => InternalServerError("Error Checking Status")
    }
  }
}