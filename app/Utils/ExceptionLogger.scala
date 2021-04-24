package Utils

import play.api.Logger

object ExceptionLogger {

  val log = Logger(this.getClass.getName)

  def newException(message: String): Exception = {
    log.error(message)
    new Exception(message)
  }
}
