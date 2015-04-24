package io.suggest.sjs.common.util

import org.scalajs.dom.console

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 15:16
 * Description: Поддержка быстрого и простого логгирования действий.
 */
object SjsLogs {

  val fmtNoEx = "%s: %s"
  val fmtEx = fmtNoEx + "\n" + fmtNoEx

}


/** Поддержка простого логгинга в js-консоль через подключение трейта. */
trait SjsLogs {

  /** Можно воткнуть сюда lazy val на стороне реализации, если ожидается слишком активный логгинг. */
  protected def loggerName = getClass.getSimpleName

  protected def fmtNoEx = SjsLogs.fmtNoEx
  protected def fmtEx   = SjsLogs.fmtEx

  protected def error(msg: String): Unit = {
    console.error(fmtNoEx, loggerName, msg)
  }
  protected def error(msg: String, ex: Throwable): Unit = {
    console.error(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  protected def warn(msg: String): Unit = {
    console.warn(fmtNoEx, loggerName, msg)
  }
  protected def warn(msg: String, ex: Throwable): Unit = {
    console.warn(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  protected def info(msg: String): Unit = {
    console.info(fmtNoEx, loggerName, msg)
  }
  protected def info(msg: String, ex: Throwable): Unit = {
    console.info(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

  protected def log(msg: String): Unit = {
    console.log(fmtNoEx, loggerName, msg)
  }
  protected def log(msg: String, ex: Throwable): Unit = {
    console.log(fmtEx, loggerName, msg, ex.getClass.getName, ex.getMessage)
  }

}

