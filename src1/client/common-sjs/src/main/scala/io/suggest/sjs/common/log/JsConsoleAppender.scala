package io.suggest.sjs.common.log

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

  override def logAppend(logMsg: LogMsg): Unit = {

    // Подбираем правильную функцию для логгирования в консоль...
    val f: (js.Any, Seq[js.Any]) => Unit = {
      logMsg.severity match {
        case Severities.Error => dom.console.error
        case Severities.Warn  => dom.console.warn
        case Severities.Info  => dom.console.info
        case Severities.Log   => dom.console.log
      }
    }

    var acc = List.empty[String]

    val nl = HtmlConstants.NEWLINE_UNIX.toString

    for (state <- logMsg.fsmState)
      acc = nl :: state :: acc

    acc ::= logMsg.onlyMainText

    for (code <- logMsg.code)
      acc ::= code

    f( acc.mkString(nl), Nil )
  }

}
