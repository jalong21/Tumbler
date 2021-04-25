package Utils

import play.api.Logger
/*
I wouldn't usually do this, but it was convenient for this project
This is for debugging, logging exceptions
 */
object ExceptionLogger {

  val log = Logger(this.getClass.getName)

  def newException(message: String): Exception = {
    log.error(message)
    new Exception(message)
  }
}
