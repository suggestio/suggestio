package util

import play.api.Logger
import io.suggest.util.LogsAbstract

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 18:13
 * Description: Добавить инстанс play-only логгера для текущего модуля.
 */

trait Logs {

  protected val LOGGER = Logger(getClass.getName)

}


trait SioutilLogs extends LogsAbstract with Logs {

  protected def debug(message: => String) {
    if (LOGGER.isDebugEnabled)
      LOGGER.debug(message)
  }

  protected def debug(message: => String, ex: Throwable) {
    if (LOGGER.isDebugEnabled)
      LOGGER.debug(message, ex)
  }

  protected def info(message: => String) {
    if (LOGGER.isInfoEnabled)
      LOGGER.info(message)
  }

  protected def info(message: => String, ex: Throwable) {
    if (LOGGER.isInfoEnabled)
      LOGGER.info(message, ex)
  }

  protected def warn(message: => String) {
    if (LOGGER.isWarnEnabled)
      LOGGER.warn(message)
  }

  protected def warn(message: => String, ex: Throwable) {
    if (LOGGER.isWarnEnabled)
      LOGGER.warn(message, ex)
  }

  protected def error(ex: Throwable) {
    if (LOGGER.isErrorEnabled)
      LOGGER.error("Undescribed error: ", ex)
  }

  protected def error(message: => String) {
    if (LOGGER.isErrorEnabled)
      LOGGER.error(message)
  }

  protected def error(message: => String, ex: Throwable) {
    if (LOGGER.isErrorEnabled)
      LOGGER.error(message, ex)
  }
}
