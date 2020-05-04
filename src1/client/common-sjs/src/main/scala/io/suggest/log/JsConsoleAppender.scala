package io.suggest.log

import io.suggest.common.html.HtmlConstants
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 16:56
  * Description: Логгирование в js-консоль.
  */
class JsConsoleAppender extends ILogAppender {

  override def logAppend(logMsgs: Seq[MLogMsg]): Unit = {
    for (logMsg <- logMsgs) {
      // Подбираем правильную функцию для логгирования в консоль...
      val f: (js.Any, Seq[js.Any]) => Unit = {
        logMsg.severity match {
          case LogSeverities.Error => dom.console.error
          case LogSeverities.Warn  => dom.console.warn
          case LogSeverities.Info  => dom.console.info
          case LogSeverities.Log   => dom.console.log
        }
      }

      var acc = List.empty[String]

      val nl = HtmlConstants.NEWLINE_UNIX.toString

      acc ::= logMsg.onlyMainText

      for (code <- logMsg.code)
        acc ::= code

      f( acc.mkString(nl), Nil )
    }
  }

}
