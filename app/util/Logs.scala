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

  protected val LOGGER = Logger(getClass)

}

trait LazyLogger {
  protected lazy val LOGGER = Logger(getClass)
}

/** trait-костыль. Подмешивается к abstract-классам из библиотек, ибо они не совместимы с play-логгером. */
trait SioutilLogs extends LogsAbstract with Logs {

  // Чтобы дважды не делать isSomethingEnabled, используем прямое обращение к логгеру slf4j.
  protected def S4JL = LOGGER.logger

  protected def trace(message: => String) {
    if (LOGGER.isTraceEnabled)
      S4JL.trace(message)
  }

  protected def trace(message: => String, ex: Throwable) {
    if (LOGGER.isTraceEnabled)
      S4JL.trace(message, ex)
  }

  protected def debug(message: => String) {
    if (LOGGER.isDebugEnabled)
      S4JL.debug(message)
  }

  protected def debug(message: => String, ex: Throwable) {
    if (LOGGER.isDebugEnabled)
      S4JL.debug(message, ex)
  }

  protected def info(message: => String) {
    if (LOGGER.isInfoEnabled)
      S4JL.info(message)
  }

  protected def info(message: => String, ex: Throwable) {
    if (LOGGER.isInfoEnabled)
      S4JL.info(message, ex)
  }

  protected def warn(message: => String) {
    if (LOGGER.isWarnEnabled)
      S4JL.warn(message)
  }

  protected def warn(message: => String, ex: Throwable) {
    if (LOGGER.isWarnEnabled)
      S4JL.warn(message, ex)
  }

  protected def error(ex: Throwable) {
    if (LOGGER.isErrorEnabled)
      S4JL.error("Undescribed error: ", ex)
  }

  protected def error(message: => String) {
    if (LOGGER.isErrorEnabled)
      S4JL.error(message)
  }

  protected def error(message: => String, ex: Throwable) {
    if (LOGGER.isErrorEnabled)
      S4JL.error(message, ex)
  }
}
