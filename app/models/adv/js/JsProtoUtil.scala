package models.adv.js

import io.suggest.model.EsModel.FieldsJsonAcc
import models.JsRawCode
import play.api.libs.json.{JsString, JsObject}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 19:11
 * Description: Заготовки js-моделей протокола общения с adv-фронтендами.
 */

/** Для запросов к веб-морде используется вот эта утиль. */
trait AskBuilderUtil {

  def sioPrJsName = "SioPR"
  
  def onSuccessJson(name: String, args: FieldsJsonAcc): JsObject = {
    JsObject(Seq(
      "replyTo" -> JsString(name),
      "status"  -> JsString("success"),
      "args"    -> JsObject(args)
    ))
  }

  def onErrorJson(name: String, reason: String, args: FieldsJsonAcc = Nil): JsObject = {
    JsObject(Seq(
      "replyTo" -> JsString(name),
      "status"  -> JsString("error"),
      "args"    -> JsObject(
        "reason" -> JsRawCode(reason) ::
        args
      )
    ))
  }

}

/** Сборка запросов. */
trait AskBuilder extends AskBuilderUtil {
  /** Некое название экшена. Это то, что фигурирует как идентификатор в запросах-ответах. */
  def action: String

  def onSuccessArgs: List[String]
  def onSuccessReply: JsObject = onSuccessJson(action, onSuccessJsonArgs)
  def onSuccessCallbackBuilder(sb: StringBuilder): StringBuilder = {
    sb.append("function(ws")
    onSuccessArgs.foreach { argName =>
      sb.append(',').append(argName)
    }
    sb.append("){ws.send(").append(onSuccessReply).append(");}")
  }
  def onSuccessJson(args: FieldsJsonAcc): JsObject = onSuccessJson(action, args)
  def onSuccessJsonArgs: FieldsJsonAcc = {
    onSuccessArgs
      .map { argName =>  argName -> JsRawCode(argName) }
  }

  def onErrorReply: JsObject = onErrorJson(action, "reason")
  def onErrorCallbackBuilder(sb: StringBuilder): StringBuilder = {
    sb.append("function(ws,reason){ws.send(")
      .append(onErrorReply)
      .append(");}")
  }

  def onErrorArgs: List[String] = List("reason")

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

