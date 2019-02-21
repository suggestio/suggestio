package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.JsCommand._
import io.suggest.adv.ext.model.{MJsCmdType, MJsCmdTypes}
import io.suggest.sjs.common.model.FromStringT

import scala.scalajs.js.{Any, Dictionary, WrappedDictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 12:20
 * Description: Модель, описывающая входящую команду от ws-сервера.
 */

object IJsCmd extends FromStringT {

  override type T = IJsCmd

  def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]]: WrappedDictionary[Any]
    d.get(TYPE_FN)
      .map(_.toString)
      .flatMap(MJsCmdTypes.withValueOpt)
      .flatMap { cmdType =>
        cmdType match {

          case MJsCmdTypes.JavaScript =>
            d.get(JS_CODE_FN).map { jsCodeDyn =>
              MJsCommand(
                jsCode = jsCodeDyn.toString,
                isPopup = d.get(IS_POPUP_FN)
                  .fold(false)(_.asInstanceOf[Boolean])
              )
            }

          case MJsCmdTypes.Action =>
            d.get(MCTX_FN).map { mctxDyn =>
              MActionCmd(
                mctx    = MJsCtx.fromJson(mctxDyn),
                replyTo = d.get(REPLY_TO_FN).map(_.toString)
              )
            }

        }
      }
      .get
  }

}



/** Абстрактный интерфейс команды. */
sealed trait IJsCmd {
  def ctype: MJsCmdType
}

/**
 * Экземпляр полученной js-команды (js-кода) по ws от sio-сервера.
 * @param jsCode Тело команды в виде строки.
 */
case class MJsCommand(jsCode: String, isPopup: Boolean) extends IJsCmd {
  override def ctype = MJsCmdTypes.JavaScript
}

case class MActionCmd(mctx: MJsCtx, replyTo: Option[String]) extends IJsCmd {
  override def ctype = MJsCmdTypes.Action
}
