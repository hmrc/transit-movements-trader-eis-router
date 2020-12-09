package logging

import play.api.Logger

trait Logging {

  protected val logger: Logger = Logger(s"application.${this.getClass.getCanonicalName}")
}
