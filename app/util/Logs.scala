package util

import play.api.Logger
import io.suggest.util.LogsAbstract

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 18:13
 * Description: Добавить инстанс логгера для модуля.
 */

trait Logs {

  protected val logger = Logger(getClass.getName)

}


trait SioutilLogs extends LogsAbstract with Logs {

  protected def debug(message: => String) {
    if (logger.isDebugEnabled)
      logger.debug(message)
  }

  protected def debug(message: => String, ex: Throwable) {
    if (logger.isDebugEnabled)
      logger.debug(message, ex)
  }

  protected def info(message: => String) {
    if (logger.isInfoEnabled)
      logger.info(message)
  }

  protected def info(message: => String, ex: Throwable) {
    if (logger.isInfoEnabled)
      logger.info(message, ex)
  }

  protected def warn(message: => String) {
    if (logger.isWarnEnabled)
      logger.warn(message)
  }

  protected def warn(message: => String, ex: Throwable) {
    if (logger.isWarnEnabled)
      logger.warn(message, ex)
  }

  protected def error(ex: Throwable) {
    if (logger.isErrorEnabled)
      logger.error("Undescribed error: ", ex)
  }

  protected def error(message: => String) {
    if (logger.isErrorEnabled)
      logger.error(message)
  }

  protected def error(message: => String, ex: Throwable) {
    if (logger.isErrorEnabled)
      logger.error(message, ex)
  }
}
