package models.adv.js

import play.api.libs.json.{JsValue, JsString, JsObject}

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
      .append(JsString("replyTo")).append(':').append(JsString(action)).append(',')
      .append(JsString("status")).append(':').append(JsString(status)).append(',')
      .append(JsString("args")).append(':').append('{')
  }
  private def afterSomethingJson(sb: StringBuilder): StringBuilder = {
    sb.append('}')    // args{}
      .append('}')    // msg{}
  }

  def onSuccessArgsList: List[String]
  def onSuccessArgs(sb: StringBuilder): StringBuilder
  def onSuccessCallbackBuilder(sb: StringBuilder): StringBuilder = {
    sb.append("function(ws")
    onSuccessArgsList.foreach { argName =>
      sb.append(',').append(argName)
    }
    sb.append("){ws.send(")
    // Сборка ответа
    onSomethingJson("success", sb)
    onSuccessArgs(sb)
    afterSomethingJson(sb)
    // Завершена сборка ответа
    sb.append(");}")
  }

  def onErrorCallbackBuilder(sb: StringBuilder): StringBuilder = {
    sb.append("function(ws,reason){ws.send(")
    onSomethingJson("error", sb)
    onErrorArgs(sb)
    afterSomethingJson(sb)
    sb.append(");}")
  }
  def onErrorArgs(sb: StringBuilder): StringBuilder = {
    val r = "reason"
    sb.append(JsString(r)).append(':').append(r)
  }


  /**
   * В конце сборки каждого запроса вызывается ".execute(onSuccess, onError)".
   * Тут метод, занимающийся генерацией этого кода.
   * @param sb
   * @return
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


/** Заготовка для быстрой сборки статических компаньонов классов js-протокола, умеющих делать unapply() из JSON'а. */
trait StaticUnapplier extends IAction {

  /** Класс-компаньон. */
  type T

  /** Результат, возвращаемый из unapply(). Если больше одного аргумента, то нужен кортеж. */
  type Tu

  /** Этот элемент точно подходит. Нужно десериализовать данные из него. */
  def fromJs(json: JsValue): Tu

  /** Статус, который требуется классом-компаньоном. */
  def statusExpected: String

  def isStatusExpected(json: JsObject): Boolean = {
    (json \ "status").toString() == statusExpected
  }

  def isReplyToMe(json: JsObject): Boolean = {
    (json \ "replyTo").toString == action
  }

  def unapplyJs(json: JsObject): Option[Tu] = {
    if (isStatusExpected(json)  &&  isReplyToMe(json)) {
      Some(fromJs(json \ "args"))
    } else {
      None
    }
  }

  def unapply(a: Any): Option[Tu] = {
    a match {
      case jso: JsObject    => unapplyJs(jso)
      case _                => None
    }
  }

}

