package models.adv.js

import models.adv.MExtServices
import models.adv.MExtServices.MExtService
import models.adv.js.ctx.JsCtx_t
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
    sb.append("){ws.send(JSON.stringify(")
    // Сборка ответа
    onSomethingJson("success", sb)
    onSuccessArgs(sb)
    afterSomethingJson(sb)
    // Завершена сборка ответа
    sb.append("));}")
  }

  def onErrorCallbackBuilder(sb: StringBuilder): StringBuilder = {
    sb.append("function(ws,reason){ws.send(JSON.stringify(")
    onSomethingJson("error", sb)
    onErrorArgs(sb)
    afterSomethingJson(sb)
    sb.append("));}")
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
    if (isReplyToMe(json) && isStatusExpected(json)) {
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


trait Ctx2Fields {
  def CTX2 = "ctx2"
}

/** Интерфейс экземпляра успешного исполнения приказа. */
trait ISuccess {
  def service: MExtService
  def ctx2: JsCtx_t
}

/** Реализация StaticUnapplier заточенный под типичный success-ответ: (service, ctx2). */
trait ServiceAndCtxStaticUnapplier extends StaticUnapplier with Ctx2Fields {
  override type T <: ISuccess
  override type Tu = (MExtService, JsCtx_t)
  override def statusExpected = "success"

  def apply(service: MExtService, ctx2: JsCtx_t): T

  implicit val hpsReads: Reads[T] = {
    val s =
      ServiceStatic.serviceFieldReads and
      (JsPath \ CTX2).read[JsObject]
    s(apply _)
  }

  /** Этот элемент точно подходит. Нужно десериализовать данные из него. */
  override def fromJs(json: JsValue): Tu = {
    val v = json.validate[T].get
    (v.service, v.ctx2)
  }

}

/** Интерфейс экземпляров ошибок. */
trait IError {
  def service: MExtService
  def reason: String
}


object StaticErrorUnapplier {
  type Tu = String
  implicit val seReads: Reads[String] = {
    (JsPath \ "reason").read[String]
  }
}
trait StaticErrorUnapplier extends StaticUnapplier {
  override type Tu = StaticErrorUnapplier.Tu
  override def statusExpected: String = "error"
  override def fromJs(json: JsValue): Tu = {
    json.validate(StaticErrorUnapplier.seReads)
      .get
  }
}

object StaticServiceErrorUnapplier {
  type Tu = (MExtService, String)
  implicit val sseReads: Reads[Tu] = {
    val p = ServiceStatic.serviceFieldReads and
      StaticErrorUnapplier.seReads
    p.apply(Tuple2(_, _))
  }
}
trait StaticServiceErrorUnapplier extends StaticUnapplier {
  override type Tu = StaticServiceErrorUnapplier.Tu
  override def statusExpected = "error"
  override def fromJs(json: JsValue): Tu = {
    json.validate(StaticServiceErrorUnapplier.sseReads)
      .get
  }
}


object ServiceStatic {
  val SERVICE = "service"

  implicit val serviceFieldReads = (JsPath \ SERVICE).read[String].map(MExtServices.withName(_): MExtService)
}

/** В параметры onSuccess и onError колбэков добавляется параметр service для уточнения целевого актора. */
trait CallbackServiceAskBuilder extends AskBuilder {
  import ServiceStatic._

  def service: MExtService

  private def appendService(sb: StringBuilder): StringBuilder = {
    sb.append(JsString(SERVICE)).append(':').append(JsString(service.strId))
  }

  override def onSuccessArgs(sb: StringBuilder): StringBuilder = {
    appendService(sb)
  }

  override def onErrorArgs(sb: StringBuilder): StringBuilder = {
    super.onErrorArgs(sb)
      .append(',')
    appendService(sb)
  }
}


/** Добавить вызов ".service(...)". */
trait ServiceCall extends CallbackServiceAskBuilder {
  override def buildJsCodeBody(sb: StringBuilder): StringBuilder = {
    super.buildJsCodeBody(sb)
      .append(".service(")
      .append(JsString(service.strId))
      .append(')')
  }
}

