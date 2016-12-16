package io.suggest.sjs.common.i18n

import io.suggest.i18n.IMessage

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:09
  * Description: JS-интерфейсы для client-side словаря локализации.
  */

/** интерфейс для messages-объекта в рамках одного языка. */
@js.native
sealed trait JsMessagesSingleLang extends js.Object {

  /**
    * Рендер одного сообщения.
    *
    * @param code Код по messages.
    * @param args Аргументы рендера, если есть.
    * @return Строка с результатом.
    */
  def apply(code: String, args: js.Any*): String = js.native

  /**
    * The JavaScript function stores the messages map in a messages property that is publicly accessible
    * so you can update the messages without reloading the page.
    */
  val messages: js.Dictionary[String] = js.native

}


/** Поддержка apply() метода, поглащающего инстансы [[IMessage]]. */
trait JsMessager {

  def Messages: JsMessagesSingleLang

  def apply(fe: IMessage): String = {
    Messages(fe.message, fe.args)
  }

}
