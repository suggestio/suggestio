package io.suggest.sjs.common.i18n

import io.suggest.i18n.{I18nConst, IMessage, MessagesF_t}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:09
  * Description: JS-интерфейсы для client-side словаря локализации.
  */

/** интерфейс для messages-объекта в рамках одного языка. */
@js.native
sealed trait IJsMessagesSingleLang extends js.Object {

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


/**
  * Фасад к нативному глобальному инстансу window._SioMessages.
  */
@js.native
@JSName( I18nConst.MESSAGES_JSNAME )
object JsMessagesSingleLangNative extends IJsMessagesSingleLang


/** Основной доступ к локализациям в рамках одного языка.
  * Легко импортировать, легко заюзать.
  */
object Messages {

  /** Локализовать инстанс IMessage. */
  def apply(fe: IMessage): String = {
    apply1(fe.message, fe.args)
  }

  /** Локализовать сообщение по коду и опциональным аргументам. */
  def apply(message: String, args: Any*): String = {
    apply1(message, args.asInstanceOf[Seq[js.Any]])
  }

  def apply1(message: String, args: Seq[Any]): String = {
    // Шаманство с аргументами из-за конфликта между Any, AnyRef и js.Any.
    val argsJs = args.asInstanceOf[Seq[js.Any]]
    JsMessagesSingleLangNative(message, argsJs: _*)
  }

  /** Вернуть инстанс MessagesF_t, который можно передавать в т.ч. кросс-платформенный код. */
  def f: MessagesF_t = apply1

}