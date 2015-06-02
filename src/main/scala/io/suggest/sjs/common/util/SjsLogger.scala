package io.suggest.sjs.common.util

import org.scalajs.dom.console

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 15:16
 * Description: Поддержка быстрого и простого логгирования действий.
 */
object SjsLogger {

  val fmtNoEx = "%s: %s"
  val fmtEx = fmtNoEx + "\n" + fmtNoEx

}


/** Интерфейс для конкретных логгеров для scala-js. */
trait ISjsLogger {
  def error(msg: String): Unit
  def error(msg: String, ex: Throwable): Unit

  def warn(msg: String): Unit
  def warn(msg: String, ex: Throwable): Unit

  def info(msg: String): Unit
  def info(msg: String, ex: Throwable): Unit

  def log(msg: String): Unit
  def log(msg: String, ex: Throwable): Unit
}


/** Поддержка простого логгинга в js-консоль через подключение этого трейта. */
trait SjsLogger extends ISjsLogger {

  /** Можно воткнуть сюда lazy val на стороне реализации, если ожидается слишком активный логгинг. */
  def loggerName = getClass.getSimpleName

  def fmtNoEx = SjsLogger.fmtNoEx
  def fmtEx   = SjsLogger.fmtEx

  override def error(msg: String): Unit = {
    console.error(fmtNoEx, loggerName, msg)
  }
  override def error(msg: String, ex: Throwable): Unit = {
    console.error(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  override def warn(msg: String): Unit = {
    console.warn(fmtNoEx, loggerName, msg)
  }
  override def warn(msg: String, ex: Throwable): Unit = {
    console.warn(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  override def info(msg: String): Unit = {
    console.info(fmtNoEx, loggerName, msg)
  }
  override def info(msg: String, ex: Throwable): Unit = {
    console.info(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  override def log(msg: String): Unit = {
    console.log(fmtNoEx, loggerName, msg)
  }
  override def log(msg: String, ex: Throwable): Unit = {
    console.log(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

}

/** Враппер над другим логгером. Т.е. используется логгинг от другой реализации.
  * Бывает полезно для анонимных реализаций inner-классов, требующих [[ISjsLogger]]. */
trait SjsLogWrapper extends ISjsLogger {
  def _LOGGER: ISjsLogger

  override def log(msg: String)                   = _LOGGER.log(msg)
  override def log(msg: String, ex: Throwable)    = _LOGGER.log(msg, ex)
  override def info(msg: String)                  = _LOGGER.info(msg)
  override def info(msg: String, ex: Throwable)   = _LOGGER.info(msg, ex)
  override def warn(msg: String)                  = _LOGGER.warn(msg)
  override def warn(msg: String, ex: Throwable)   = _LOGGER.warn(msg, ex)
  override def error(msg: String)                 = _LOGGER.error(msg)
  override def error(msg: String, ex: Throwable)  = _LOGGER.error(msg, ex)
}
