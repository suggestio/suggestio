package io.suggest.util

import org.slf4j.LoggerFactory

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.13 11:52
 * Description: http://stackoverflow.com/a/2087048
 */
trait Logs extends LogsAbstract {
  protected val logger = LoggerFactory.getLogger(getClass.getName)

  protected def debug(message: => String) = if (logger.isDebugEnabled) logger.debug(message)
  protected def debug(message: => String, ex:Throwable) = if (logger.isDebugEnabled) logger.debug(message,ex)

  protected def info(message: => String) = if (logger.isInfoEnabled) logger.info(message)
  protected def info(message: => String, ex:Throwable) = if (logger.isInfoEnabled) logger.info(message,ex)

  protected def warn(message: => String) = if (logger.isWarnEnabled) logger.warn(message)
  protected def warn(message: => String, ex:Throwable) = if (logger.isWarnEnabled) logger.warn(message,ex)

  protected def error(ex:Throwable) = if (logger.isErrorEnabled) logger.error(ex.toString,ex)
  protected def error(message: => String) = if (logger.isErrorEnabled) logger.error(message)
  protected def error(message: => String, ex:Throwable) = if (logger.isErrorEnabled) logger.error(message,ex)
}

// То же самое, что и Logs, но добавляется префикс.
trait LogsPrefixed extends Logs {
  protected val logPrefix: String

  override protected def debug(message: => String) = super.debug(logPrefix + message)
  override protected def debug(message: => String, ex: Throwable)  = super.debug(logPrefix + message, ex)

  override protected def info(message: => String) = super.info(logPrefix + message)
  override protected def info(message: => String, ex: Throwable) = super.info(logPrefix + message, ex)

  override protected def warn(message: => String) = super.warn(logPrefix + message)
  override protected def warn(message: => String, ex: Throwable) = super.warn(logPrefix + message, ex)

  override protected def error(ex: Throwable) = super.error(logPrefix + ex)
  override protected def error(message: => String) = super.error(logPrefix + message)

  override protected def error(message: => String, ex: Throwable) = super.error(logPrefix + message, ex)
}

/**
 * Абстрактный логгер появился как костыль для play-framework,
 * где не осилили интерфейс логгеров и изобрели свой уникальный LoggerLike.
 */
trait LogsAbstract {

  protected def debug(message: => String)
  protected def debug(message: => String, ex:Throwable)

  protected def debugValue[T](valueName: String, value: => T):T = {
    val result:T = value
    debug(valueName + " == " + result.toString)
    result
  }


  protected def info(message: => String)
  protected def info(message: => String, ex:Throwable)

  protected def warn(message: => String)
  protected def warn(message: => String, ex:Throwable)

  protected def error(ex:Throwable)
  protected def error(message: => String)
  protected def error(message: => String, ex:Throwable)

}