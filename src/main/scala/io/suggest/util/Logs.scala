package io.suggest.util

import org.slf4j.LoggerFactory

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.13 11:52
 * Description: http://stackoverflow.com/a/2087048
 */
trait Logs extends LogsAbstract {
  @transient protected lazy val logger = LoggerFactory.getLogger(getClass.getName)

  protected def trace(message: => String) = if (logger.isTraceEnabled) logger.trace(message)
  protected def trace(message: => String, ex:Throwable) = if (logger.isTraceEnabled) logger.trace(message, ex)

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

  override protected def trace(message: => String) = super.trace(logPrefix + message)
  override protected def trace(message: => String, ex: Throwable) = super.trace(logPrefix + message, ex)

  override protected def debug(message: => String) = super.debug(logPrefix + message)
  override protected def debug(message: => String, ex: Throwable)  = super.debug(logPrefix + message, ex)

  override protected def info(message: => String) = super.info(logPrefix + message)
  override protected def info(message: => String, ex: Throwable) = super.info(logPrefix + message, ex)

  override protected def warn(message: => String) = super.warn(logPrefix + message)
  override protected def warn(message: => String, ex: Throwable) = super.warn(logPrefix + message, ex)

  override protected def error(ex: Throwable) = super.error(logPrefix, ex)
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

  protected def trace(message: => String)
  protected def trace(message: => String, ex:Throwable)
}


// Бывает, что нужен логгер в виде объекта.
class LogsImpl(className: String) {
  def this(clazz: Class[_]) = this(clazz.getName)

  @transient val _logger = LoggerFactory.getLogger(className)

  def debug(message: => String) = if (_logger.isDebugEnabled) _logger.debug(message)
  def debug(message: => String, ex:Throwable) = if (_logger.isDebugEnabled) _logger.debug(message,ex)

  def info(message: => String) = if (_logger.isInfoEnabled) _logger.info(message)
  def info(message: => String, ex:Throwable) = if (_logger.isInfoEnabled) _logger.info(message,ex)

  def warn(message: => String) = if (_logger.isWarnEnabled) _logger.warn(message)
  def warn(message: => String, ex:Throwable) = if (_logger.isWarnEnabled) _logger.warn(message,ex)

  def error(ex:Throwable) = if (_logger.isErrorEnabled) _logger.error(ex.toString,ex)
  def error(message: => String) = if (_logger.isErrorEnabled) _logger.error(message)
  def error(message: => String, ex:Throwable) = if (_logger.isErrorEnabled) _logger.error(message,ex)

  def trace(message: => String) = if (_logger.isTraceEnabled) _logger.trace(message)
  def trace(message: => String, ex:Throwable) = if(_logger.isTraceEnabled) _logger.trace(message, ex)

  def isTraceEnabled = _logger.isTraceEnabled
  def isDebugEnabled = _logger.isDebugEnabled
  def isInfoEnabled  = _logger.isInfoEnabled
  def isWarnEnabled  = _logger.isWarnEnabled
  def isErrorEnabled = _logger.isErrorEnabled
}

