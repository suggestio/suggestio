package models.adv.js

import io.suggest.model.EnumMaybeWithName
import models.adv.MExtService
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 19:11
 * Description: Заготовки js-моделей протокола общения с adv-фронтендами.
 */

trait IAction {
  /** Некое название экшена. Это то, что фигурирует как идентификатор в запросах-ответах. */
  def action: String
}

/** trait-шаблон для сборки запросов, т.е. классов, генерящих js-код. */
trait AskBuilder extends IAction {

  def sioPrJsName = "SioPR"

  private def onSomethingJson(status: String, sb: StringBuilder): StringBuilder = {
    sb.append('{')
      .append(JsString(Answer.REPLY_TO_FN)).append(':').append(JsString(action)).append(',')
      .append(JsString(Answer.STATUS_FN)).append(':').append(JsString(status))
  }
  private def afterSomethingJson(sb: StringBuilder): StringBuilder = {
    sb.append('}')    // msg{}
  }

  def onSuccessArgsList: List[String] = List(Answer.CTX2)

  def onSuccessArgs(sb: StringBuilder): StringBuilder = {
    onSuccessArgsList.foreach { fn =>
      sb.append(',')
        .append(JsString(fn))
        .append(':')
        .append(fn)
    }
    sb
  }

  def onSuccessCallbackBuilder(sb: StringBuilder): StringBuilder = {
    sb.append("function(ws")
    onSuccessArgsList.foreach { argName =>
      sb.append(',').append(argName)
    }
    sb.append("){ws.send(JSON.stringify(")
    // Сборка ответа
    onSomethingJson(AnswerStatuses.Success.jsStr, sb)
    onSuccessArgs(sb)
    // Завершить оформление кода onSuccess().
    afterSomethingJson(sb)
    // Завершена сборка ответа: завершаем рендер вызова function(){}.
    sb.append("));}")
  }

  def onErrorCallbackBuilder(sb: StringBuilder): StringBuilder = {
    sb.append("function(ws,reason){ws.send(JSON.stringify(")
    onSomethingJson(AnswerStatuses.Error.jsStr, sb)
    onErrorArgs(sb)
    afterSomethingJson(sb)
    sb.append("));}")
  }
  def onErrorArgs(sb: StringBuilder): StringBuilder = {
    val r = "reason"
    sb.append(',')
      .append(JsString(r))
      .append(':')
      .append(r)
  }


  /**
   * В конце сборки каждого запроса вызывается ".execute(onSuccess, onError)".
   * Тут метод, занимающийся генерацией этого кода.
   * @param sb Аккамулятор строки.
   * @return Экземпляр StringBuilder, желательно тот же самый, что и sb.
   */
  def executeCallBuilder(sb: StringBuilder): StringBuilder = {
    sb.append(".execute(")
    onSuccessCallbackBuilder(sb)
      .append(',')
    onErrorCallbackBuilder(sb)
      .append(')')
  }

  /** Начальный размер StringBuilder'а. */
  def sbInitSz: Int = 256

  /**
   * Генерация основного js-кода.
   * Этог метод должен быть перезаписан в наследии, чтобы добиться добавления js-кода между начальным SioPR и
   * финальным .execute().
   * @param sb Начальный аккамулятор.
   * @return Финальный аккамулятор.
   */
  def buildJsCodeBody(sb: StringBuilder): StringBuilder = sb

  /** Сборка строки, которая является финальным кодом js, который надо исполнить на клиенте. */
  def js: String = {
    val sb = new StringBuilder(sbInitSz, sioPrJsName)
    buildJsCodeBody(sb)
    executeCallBuilder(sb)
    sb.append(';')
      .toString()
  }

  /** Клиенту через websocket приходит json, содержащий команду. Тут сборка одного json-пакета с js-командой. */
  def jsJson: JsObject = {
    JsObject(Seq(
      "type" -> JsString("js"),
      "data" -> JsString(js)
    ))
  }

  override def toString: String = js
}


/** В параметры onSuccess и onError колбэков добавляется параметр service для уточнения целевого актора. */
trait ServiceAskBuilder extends AskBuilder {
  def service: MExtService

  private def appendService(sb: StringBuilder): StringBuilder = {
    sb.append(',')
      .append(JsString(ServiceAnswer.SERVICE_FN))
      .append(':')
      .append(JsString(service.strId))
  }

  override def onSuccessArgs(sb: StringBuilder): StringBuilder = {
    super.onSuccessArgs(sb)
    appendService(sb)
  }

  override def onErrorArgs(sb: StringBuilder): StringBuilder = {
    super.onErrorArgs(sb)
    appendService(sb)
  }
}


/** Добавить вызов ".service(...)". */
trait InServiceAskBuilder extends ServiceAskBuilder {
  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    super.buildJsCodeBody(sb)
      .append(".service(")
      .append(JsString(service.strId))
      .append(')')
  }
}


/** Статусы ответов js серверу. */
object AnswerStatuses extends Enumeration with EnumMaybeWithName {

  protected abstract sealed class Val(val jsStr: String) extends super.Val(jsStr) {
    def isSuccess: Boolean
  }

  type AnswerStatus = Val
  override type T = AnswerStatus

  val Success: AnswerStatus = new Val("success") {
    override def isSuccess = true
  }

  val Error: AnswerStatus   = new Val("error") {
    override def isSuccess = false
  }

}

