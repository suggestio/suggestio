package io.suggest.util

import org.slf4j.LoggerFactory

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.13 11:52
 * Description: http://stackoverflow.com/a/2087048
 */
trait Logs extends LogsAbstract {
  @transient protected val _LOGGER = LoggerFactory.getLogger(getClass.getName)

  protected def trace(message: => String) = if (_LOGGER.isTraceEnabled) _LOGGER.trace(message)
  protected def trace(message: => String, ex:Throwable) = if (_LOGGER.isTraceEnabled) _LOGGER.trace(message, ex)

  protected def debug(message: => String) = if (_LOGGER.isDebugEnabled) _LOGGER.debug(message)
  protected def debug(message: => String, ex:Throwable) = if (_LOGGER.isDebugEnabled) _LOGGER.debug(message,ex)

  protected def info(message: => String) = if (_LOGGER.isInfoEnabled) _LOGGER.info(message)
  protected def info(message: => String, ex:Throwable) = if (_LOGGER.isInfoEnabled) _LOGGER.info(message,ex)

  protected def warn(message: => String) = if (_LOGGER.isWarnEnabled) _LOGGER.warn(message)
  protected def warn(message: => String, ex:Throwable) = if (_LOGGER.isWarnEnabled) _LOGGER.warn(message,ex)

  protected def error(ex:Throwable) = if (_LOGGER.isErrorEnabled) _LOGGER.error(ex.toString,ex)
  protected def error(message: => String) = if (_LOGGER.isErrorEnabled) _LOGGER.error(message)
  protected def error(message: => String, ex:Throwable) = if (_LOGGER.isErrorEnabled) _LOGGER.error(message,ex)

  def isTraceEnabled = _LOGGER.isTraceEnabled
  def isDebugEnabled = _LOGGER.isDebugEnabled
  def isInfoEnabled  = _LOGGER.isInfoEnabled
  def isWarnEnabled  = _LOGGER.isWarnEnabled
  def isErrorEnabled = _LOGGER.isErrorEnabled
}


// То же самое, что и Logs, но добавляется префикс.
trait LogsPrefixed extends Logs {
  protected def logPrefix: String

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
 * где, из-за проблем с интерфейсами логгеров, используется изобрели свой интерфейс LoggerLike.
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

  def isTraceEnabled: Boolean
  def isDebugEnabled: Boolean
  def isInfoEnabled:  Boolean
  def isWarnEnabled:  Boolean
  def isErrorEnabled: Boolean
}


/** Бывает, что нужен логгер в виде собственного объекта. */
class LogsImpl(className: String) extends LogsAbstract {
  def this(clazz: Class[_]) = this(clazz.getName)

  @transient val _LOGGER = LoggerFactory.getLogger(className)

  def debug(message: => String) = if (_LOGGER.isDebugEnabled) _LOGGER.debug(message)
  def debug(message: => String, ex:Throwable) = if (_LOGGER.isDebugEnabled) _LOGGER.debug(message, ex)

  def info(message: => String) = if (_LOGGER.isInfoEnabled) _LOGGER.info(message)
  def info(message: => String, ex:Throwable) = if (_LOGGER.isInfoEnabled) _LOGGER.info(message, ex)

  def warn(message: => String) = if (_LOGGER.isWarnEnabled) _LOGGER.warn(message)
  def warn(message: => String, ex:Throwable) = if (_LOGGER.isWarnEnabled) _LOGGER.warn(message, ex)

  def error(ex:Throwable) = if (_LOGGER.isErrorEnabled) _LOGGER.error(ex.toString, ex)
  def error(message: => String) = if (_LOGGER.isErrorEnabled) _LOGGER.error(message)
  def error(message: => String, ex:Throwable) = if (_LOGGER.isErrorEnabled) _LOGGER.error(message, ex)

  def trace(message: => String) = if (_LOGGER.isTraceEnabled) _LOGGER.trace(message)
  def trace(message: => String, ex:Throwable) = if(_LOGGER.isTraceEnabled) _LOGGER.trace(message, ex)

  def isTraceEnabled = _LOGGER.isTraceEnabled
  def isDebugEnabled = _LOGGER.isDebugEnabled
  def isInfoEnabled  = _LOGGER.isInfoEnabled
  def isWarnEnabled  = _LOGGER.isWarnEnabled
  def isErrorEnabled = _LOGGER.isErrorEnabled
}


/** Используем scala macros логгирование, которое НЕ порождает вообще лишнего мусора и куч анонимных функций.
  * Трейт подмешивается в класс, и затем нужно сделать "import LOGGER._". Это импортнёт scala-макросы как методы. */
trait MacroLogsImplMin {
  val LOGGER = com.typesafe.scalalogging.slf4j.Logger(LoggerFactory.getLogger(getClass))
}

/** Враппер над [[io.suggest.util.MacroLogsImplMin]], который добавляется короткие вызовы для isXXXXEnabled(). */
trait MacroLogsImpl extends MacroLogsImplMin {
  def isTraceEnabled = LOGGER.underlying.isTraceEnabled
  def isDebugEnabled = LOGGER.underlying.isDebugEnabled
  def isInfoEnabled  = LOGGER.underlying.isInfoEnabled
  def isWarnEnabled  = LOGGER.underlying.isWarnEnabled
  def isErrorEnabled = LOGGER.underlying.isErrorEnabled
}

