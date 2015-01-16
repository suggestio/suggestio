package models.adv.js

import models.adv.js.ctx.JsCtx_t
import play.api.libs.json._
import play.api.libs.functional.syntax._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.01.15 14:51
 * Description: Парсинг ответов в рамках JSON-протокола связи.
 */

object Answer {

  // Названия полей JSON-ответа.
  /** Поле с JSON нового состояние системы (новым контекстом). */
  val CTX2 = "ctx2"

  /** Статус: успех, ошибка, ... */
  val STATUS_FN     = "status"

  /** На какое действие ответ. Для самоконтроля. */
  val REPLY_TO_FN   = "replyTo"

  implicit val contextReads: Reads[JsCtx_t] = {
    (JsPath \ CTX2)
      .read[JsCtx_t]
  }

  /** JSON-парсер для поля статуса. */
  implicit val statusReads: Reads[AnswerStatus] = {
    (JsPath \ STATUS_FN)
      .read[String]
      .map(AnswerStatuses.withName(_): AnswerStatus)
  }

  val replyToReads: Reads[String] = {
    (JsPath \ REPLY_TO_FN)
      .read[String]
  }

  /** JSON-парсер для ответов. */
  implicit val answerReads: Reads[Answer] = {
    val answerReader = statusReads  and  replyToReads  and  contextReads
    answerReader(apply _)
  }

  type Tu = (AnswerStatus, String, JsCtx_t)

  /** Прозрачное приведение JsValue к содержимому Answer'а. */
  def unapply(json: JsValue): Option[Tu] = {
    json.asOpt[Answer]
      .flatMap { unapply }
  }

  def unapply(any: Any): Option[Tu] = {
    any match {
      case jsv: JsValue => unapply(jsv)
      case a: Answer    => unapply(a)
      case _            => None
    }
  }

}


/** Интерфейс ответа. */
trait IAnswer {

  /** Статус ответа. */
  def status    : AnswerStatus

  /** По какому экшену ответ? (См. поле action в [[AskBuilder]]). */
  def replyTo   : String

  /** Обновлённое состояние. */
  def ctx2      : JsCtx_t

}


/** Экземпляр одного распарсенного ответа в рамках сервиса. */
case class Answer(status: AnswerStatus, replyTo: String, ctx2: JsCtx_t)
  extends IAnswer

