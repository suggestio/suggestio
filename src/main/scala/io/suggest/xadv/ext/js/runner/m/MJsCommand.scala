package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.MCommandTypesLightT
import io.suggest.adv.ext.model.JsCommand._
import io.suggest.adv.ext.model.JsCommandTypes._
import scala.scalajs.js.{WrappedDictionary, Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 12:20
 * Description: Модель, описывающая входящую команду от ws-сервера.
 */

object MJsCommand extends FromStringT {

  override type T = ICmd

  def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]]
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
case class MJsCommand(jsCode: String, isPopup: Boolean) extends ICmd {
  override def ctype = MCommandTypes.JavaScript
}

case class MActionCmd(mctx: MJsCtx, replyTo: Option[String]) extends ICmd {
  override def ctype = MCommandTypes.Action
}

/** Распарсенная команда чтения данных из хранилища браузера по ключу. */
case class MGetStorageCmd(key: String, replyTo: String) extends ICmd {
  override def ctype = MCommandTypes.GetStorage
}

case class MSetStorageCmd(key: String, value: Option[String]) extends ICmd {
  override def ctype: MCommandType = ???
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
    def dyn2cmd(d: Dictionary[Any]): Option[ICmd]
  }

  override type T = Val

  override val JavaScript: T = new Val(CTYPE_JS) {
    override def dyn2cmd(dr: Dictionary[Any]): Option[MJsCommand] = {
      val d = dr: WrappedDictionary[Any]
      d.get(JS_CODE_FN).map { jsCodeDyn =>
        MJsCommand(
          jsCode = jsCodeDyn.toString,
          isPopup = d.get(IS_POPUP_FN)
            .fold(false)(_.asInstanceOf[Boolean])
        )
      }
    }
  }

  override val Action: T = new Val(CTYPE_ACTION) {
    override def dyn2cmd(d: Dictionary[Any]): Option[MActionCmd] = {
      val dr = d : WrappedDictionary[Any]
      dr.get(MCTX_FN).map { mctxDyn =>
        MActionCmd(
          mctx    = MJsCtx.fromJson(mctxDyn),
          replyTo = d.get(REPLY_TO_FN).map(_.toString)
        )
      }
    }
  }

  override val GetStorage: T = new Val(CTYPE_GET_STORAGE) {
    override def dyn2cmd(d: Dictionary[Any]): Option[MGetStorageCmd] = {
      val dr = d : WrappedDictionary[Any]
      dr.get(KEY_FN).map { keyRaw =>
        MGetStorageCmd(
          key     = keyRaw.toString,
          replyTo = dr(REPLY_TO_FN).toString
        )
      }
    }
  }

  override val SetStorage: T = new Val(CTYPE_SET_STORAGE) {
    override def dyn2cmd(d: Dictionary[Any]): Option[ICmd] = {
      val dr = d: WrappedDictionary[Any]
      dr.get(KEY_FN).map { k =>
        MSetStorageCmd(
          key   = k.toString,
          value = dr.get(VALUE_FN).map(_.toString)
        )
      }
    }
  }

}

