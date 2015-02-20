package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.{MCommandTypesLightT, JsCommandFieldsT}

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 12:20
 * Description: Модель, описывающая входящую команду от ws-сервера.
 */

object MJsCommand extends JsCommandFieldsT with FromStringT {

  override type T = MJsCommand

  def fromDyn(raw: js.Dynamic): MJsCommand = {
    val d = raw.asInstanceOf[js.Dictionary[String]]
    MJsCommand(
      ctype     = MCommandTypes.withName( d.get(TYPE_FN).get ),
      data      = d.get(DATA_FN).get,
      replyTo   = d.get(REPLY_TO_FN)
    )
  }

}



/**
 * Экземпляр полученной команды по ws от sio-сервера.
 * @param ctype Тип команды.
 * @param data Тело команды в виде строки.
 * @param replyTo Адресат ответа, если есть/требуется.
 */
case class MJsCommand(ctype     : MCommandType,
                      data      : String,
                      replyTo   : Option[String] = None)


/** Известные системе типы команд. */
object MCommandTypes extends MCommandTypesLightT

