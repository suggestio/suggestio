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
  protected def error(msg: String): Unit
  protected def error(msg: String, ex: Throwable): Unit

  protected def warn(msg: String): Unit
  protected def warn(msg: String, ex: Throwable): Unit

  protected def info(msg: String): Unit
  protected def info(msg: String, ex: Throwable): Unit

  protected def log(msg: String): Unit
  protected def log(msg: String, ex: Throwable): Unit
}


/** Поддержка простого логгинга в js-консоль через подключение этого трейта. */
trait SjsLogger extends ISjsLogger {

  /** Можно воткнуть сюда lazy val на стороне реализации, если ожидается слишком активный логгинг. */
  protected def loggerName = getClass.getSimpleName

  protected def fmtNoEx = SjsLogger.fmtNoEx
  protected def fmtEx   = SjsLogger.fmtEx

  override protected def error(msg: String): Unit = {
    console.error(fmtNoEx, loggerName, msg)
  }
  override protected def error(msg: String, ex: Throwable): Unit = {
    console.error(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  override protected def warn(msg: String): Unit = {
    console.warn(fmtNoEx, loggerName, msg)
  }
  override protected def warn(msg: String, ex: Throwable): Unit = {
    console.warn(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  override protected def info(msg: String): Unit = {
    console.info(fmtNoEx, loggerName, msg)
  }
  override protected def info(msg: String, ex: Throwable): Unit = {
    console.info(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  override protected def log(msg: String): Unit = {
    console.log(fmtNoEx, loggerName, msg)
  }
  override protected def log(msg: String, ex: Throwable): Unit = {
    console.log(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

}

