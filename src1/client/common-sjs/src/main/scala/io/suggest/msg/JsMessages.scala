package io.suggest.msg

import io.suggest.i18n.{I18nConst, IMessage, MessagesF_t}
import io.suggest.sjs.common.log.Log
import japgolly.univeq.UnivEq

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.util.Try

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
@JSGlobal( I18nConst.MESSAGES_JSNAME )
object JsMessagesSingleLangNative extends IJsMessagesSingleLang


/** Класс Messages для возможности переключения языков в будущем. (надо бы через JSON) */
sealed trait Messages extends Log {

  /** Локализовать инстанс IMessage. */
  def apply(fe: IMessage): String = {
    apply1(fe.message, fe.args)
  }

  /** Локализовать сообщение по коду и опциональным аргументам. */
  def apply(message: String, args: Any*): String = {
    apply1(message, args.asInstanceOf[Seq[js.Any]])
  }

  private var _suppressErrors: Boolean = false

  def apply1(message: String, args: Seq[Any]): String = {
    try {
      // Шаманство с аргументами из-за конфликта между Any, AnyRef и js.Any.
      val argsJs = args.asInstanceOf[Seq[js.Any]]
      _applyJs(message, argsJs)
    } catch { case ex: Throwable =>
      // Если с messages проблемы, то ошибки будут сыпать десятками и сотнями. Поэтому рендерим только первую ошибку, остальные глушим.
      if (!_suppressErrors) {
        _suppressErrors = true
        Try( LOG.error(ErrorMsgs.MESSAGES_FAILURE, ex, (message, args)) )
      }
      message
    }
  }

  /** Нативный запрос к JSON-словарю или JS-API. */
  protected def _applyJs(message: String, argsJs: Seq[js.Any]): String

  /** Вернуть инстанс MessagesF_t, который можно передавать в т.ч. кросс-платформенный код. */
  def f: MessagesF_t = apply1

}


/** Основной статический доступ к локализациям в рамках одного языка.
  * Легко импортировать, легко заюзать.
  */
object Messages extends Messages {

  @inline implicit def univEq: UnivEq[Messages] = UnivEq.force

  override protected def _applyJs(message: String, argsJs: Seq[js.Any]): String =
    JsMessagesSingleLangNative( message, argsJs: _* )

}
