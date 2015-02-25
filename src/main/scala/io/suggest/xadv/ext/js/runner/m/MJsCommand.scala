package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.MCommandTypesLightT
import io.suggest.adv.ext.model.JsCommand._
import io.suggest.adv.ext.model.JsCommandTypes._

import scala.scalajs.js
import scala.scalajs.js.Dictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 12:20
 * Description: Модель, описывающая входящую команду от ws-сервера.
 */

object MJsCommand extends FromStringT {

  override type T = ICmd

  def fromDyn(raw: js.Dynamic): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Dynamic]]
    d.get(TYPE_FN)
      .map(_.toString)
      .flatMap(MCommandTypes.maybeWithName)
      .flatMap { _.dyn2cmd(d) }
      .get
  }

}


/** Абстрактный интерфейс команды. */
sealed trait ICmd {
  def ctype: MCommandType
}

/**
 * Экземпляр полученной js-команды (js-кода) по ws от sio-сервера.
 * @param jsCode Тело команды в виде строки.
 */
case class MJsCommand(jsCode: String) extends ICmd {
  override def ctype = MCommandTypes.JavaScript
}

case class MActionCmd(mctx: MJsCtx, replyTo: Option[String]) extends ICmd {
  override def ctype = MCommandTypes.Action
}


/** Известные системе типы команд. */
object MCommandTypes extends MCommandTypesLightT {

  protected abstract class Val(val ctype: String) extends ValT {
    /**
     * Десериализация команды из распарсенного json'а.
     * @param d JSON-словарь с динамически-типизированными значениями.
     * @return Some(ICmd), если всё ок. Иначе None либо exception.
     */
    // TODO Наверное убрать Option?
    def dyn2cmd(d: js.Dictionary[js.Dynamic]): Option[ICmd]
  }

  override type T = Val

  override val JavaScript: T = new Val(CTYPE_JS) {
    override def dyn2cmd(d: js.Dictionary[js.Dynamic]): Option[MJsCommand] = {
      d.get(JS_CODE_FN).map { jsCodeDyn =>
        MJsCommand(
          jsCode = jsCodeDyn.toString
        )
      }
    }
  }

  override val Action: T = new Val(CTYPE_ACTION) {
    override def dyn2cmd(d: Dictionary[js.Dynamic]): Option[MActionCmd] = {
      d.get(MCTX_FN).map { mctxDyn =>
        MActionCmd(
          mctx    = MJsCtx.fromDyn(mctxDyn),
          replyTo = d.get(REPLY_TO_FN).map(_.toString)
        )
      }
    }
  }

}

