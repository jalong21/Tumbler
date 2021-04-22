package controllers

import play.api.mvc._

import javax.inject.Inject

class Health @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action {
    request => Ok("true")
  }
}