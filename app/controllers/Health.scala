package controllers

import play.api.mvc._
import services.RedisService

import javax.inject.Inject
import scala.util.{Failure, Success}


class Health @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  RedisService.setValue("health", "check")

  def index = Action {
    _ => Ok("true")
  }

  def redisCheck = Action {
    _ => RedisService.getValue("health") match {
      case Success(status: String) => Ok(status)
      case Failure(ex) => InternalServerError(s"Error Checking Status.  ${ex.getMessage}")
    }
  }
}