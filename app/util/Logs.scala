package util

import play.api.Logger
import io.suggest.util.LogsAbstract
import com.typesafe.scalalogging.slf4j.{Logger => MacroLogger}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 18:13
 * Description: Добавить инстанс play-only логгера для текущего модуля.
 */

object PlayMacroLogsImpl {
  def getLogger(clazz: Class[_]) = {
    val playLogger = Logger(clazz)
    val slf4jLogger = playLogger.logger
    MacroLogger(slf4jLogger)
  }
}

trait Logs {

  protected val LOGGER = Logger(getClass)

}

trait LazyLogger {
  protected lazy val LOGGER = Logger(getClass)
}

/** Аналог MacroLogsImpl, но использует плеевский генератор slf4j-логгеров. Скорее всего эквивалентен исходному логгеру.
  * Создан для проверки некоторых вещей, касающихся логгирования, потом можно будет безопасно удалить. */
trait PlayMacroLogsI {
  def LOGGER: MacroLogger
}
trait PlayMacroLogsDyn extends PlayMacroLogsI {
  protected def _loggerClass: Class[_] = getClass
  override def LOGGER = PlayMacroLogsImpl.getLogger(_loggerClass)
}
trait PlayMacroLogsImpl extends PlayMacroLogsDyn {
  override val LOGGER = super.LOGGER
}
trait PlayLazyMacroLogsImpl extends PlayMacroLogsDyn {
  override lazy val LOGGER = super.LOGGER
}



/** trait-костыль. Подмешивается к abstract-классам из библиотек, ибо они не совместимы с play-логгером. */
trait SioutilLogs extends LogsAbstract with Logs {

  // Чтобы дважды не делать isSomethingEnabled, используем прямое обращение к логгеру slf4j.
  protected def S4JL = LOGGER.logger

  protected def trace(message: => String) {
    if (isTraceEnabled)
      S4JL.trace(message)
  }

  protected def trace(message: => String, ex: Throwable) {
    if (isTraceEnabled)
      S4JL.trace(message, ex)
  }

  protected def debug(message: => String) {
    if (isDebugEnabled)
      S4JL.debug(message)
  }

  protected def debug(message: => String, ex: Throwable) {
    if (isDebugEnabled)
      S4JL.debug(message, ex)
  }

  protected def info(message: => String) {
    if (isInfoEnabled)
      S4JL.info(message)
  }

  protected def info(message: => String, ex: Throwable) {
    if (isInfoEnabled)
      S4JL.info(message, ex)
  }

  protected def warn(message: => String) {
    if (isWarnEnabled)
      S4JL.warn(message)
  }

  protected def warn(message: => String, ex: Throwable) {
    if (isWarnEnabled)
      S4JL.warn(message, ex)
  }

  protected def error(ex: Throwable) {
    if (isErrorEnabled)
      S4JL.error("Undescribed error: ", ex)
  }

  protected def error(message: => String) {
    if (isErrorEnabled)
      S4JL.error(message)
  }

  protected def error(message: => String, ex: Throwable) {
    if (isErrorEnabled)
      S4JL.error(message, ex)
  }

  def isTraceEnabled: Boolean = LOGGER.isTraceEnabled
  def isDebugEnabled: Boolean = LOGGER.isDebugEnabled
  def isInfoEnabled: Boolean  = LOGGER.isInfoEnabled
  def isWarnEnabled: Boolean  = LOGGER.isWarnEnabled
  def isErrorEnabled: Boolean = LOGGER.isErrorEnabled
}
