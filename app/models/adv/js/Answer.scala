package models.adv.js

import io.suggest.adv.ext.model.{JsCommand, MAnswerStatusesT}
import JsCommand._
import io.suggest.model.EnumJsonReadsT
import models.adv.js.ctx.MJsCtx
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.01.15 14:51
 * Description: Парсинг ответов в рамках JSON-протокола связи.
 */

object Answer {

  /** JSON-парсер для ответов. */
  implicit def reads: Reads[Answer] = (
    (JsPath \ REPLY_TO_FN).readNullable[String] and
    (JsPath \ MCTX_FN).read[MJsCtx]
  )(apply _)

  /** Тип значения, которое возвращает unapply. */
  type Tu = (Option[String], MJsCtx)

  /** Прозрачное приведение JsValue к содержимому Answer'а. */
  def unapply(json: JsValue): Option[Tu] = {
    json.asOpt[Answer]
      .flatMap { unapply }
  }

  def unapply(a: Any): Option[Tu] = {
    a match {
      case jsv: JsValue => unapply(jsv)
      case ans: Answer  => unapply(ans)
      case _            => None
    }
  }

}


/** Интерфейс ответа. */
trait IAnswer {

  /** id того актора, кому нужно отправить ответ. */
  def replyTo   : Option[String]

  /** Обновлённое состояние. */
  def ctx2      : MJsCtx

}


/** Экземпляр одного распарсенного ответа в рамках сервиса. */
case class Answer(replyTo: Option[String], ctx2: MJsCtx)
  extends IAnswer


/** Статусы ответов js серверу. */
object AnswerStatuses extends MAnswerStatusesT with EnumJsonReadsT

