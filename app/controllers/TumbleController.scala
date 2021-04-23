package controllers

import models.TumbleRequest
import play.api.libs.json.Json
import play.api.mvc._
import services.JobCoinTumbler

import javax.inject.Inject
import scala.concurrent.Await
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class TumbleController @Inject()(cc: ControllerComponents,
                                 tumbler: JobCoinTumbler) extends AbstractController(cc) {

  def initializeTumble() = Action {
    request => request.body.asJson
        .map( requestBody => Try(Json.fromJson[TumbleRequest](requestBody).get) match {
          case Success(tumbleRequest) => Ok(tumbler.initializeTumble(tumbleRequest))
          case Failure(_) => BadRequest("Error Parsing Request Body!")
        }).getOrElse(BadRequest("Payload Missing From Request"))
  }

  def isTumbleComplete(tumbleId: String) = Action {
    request => Await.result(tumbler.checkForCompletion(tumbleId)
        .map(completion => completion
          .map(result => Ok(result.toString))
          .getOrElse(BadRequest("TumbleId Not Found!"))), 2.seconds)
  }
}