package models.adv.js

import models.adv.{MExtServices, MExtService}
import models.adv.js.ctx.JsCtx_t
import play.api.libs.json._
import play.api.libs.functional.syntax._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.01.15 16:30
 * Description: Модель распарсенных JSON-ответов в рамках одного сервиса.
 */

object ServiceAnswer {

  /** К какому сервису (service-актору) относится ответ. */
  val SERVICE_FN    = "service"

  implicit val serviceReads: Reads[MExtService] = {
    (JsPath \ SERVICE_FN)
      .read[String]
      .map(MExtServices.withName(_): MExtService)
  }

  implicit val serviceAnswerReads: Reads[ServiceAnswer] = {
    val s = serviceReads and Answer.answerReads
    s { (service, ans)  =>  ServiceAnswer(ans.status, service, ans.replyTo, ans.ctx2) }
  }

  type Tu = (AnswerStatus, MExtService, String, JsCtx_t)

  /** Прозрачное приведение JsValue к содержимому Answer'а. */
  def unapply(json: JsValue): Option[Tu] = {
    json.asOpt[ServiceAnswer]
      .flatMap { unapply }
  }

  def unapply(any: Any): Option[Tu] = {
    any match {
      case sa: ServiceAnswer => unapply(sa)
      case jsv: JsValue      => unapply(jsv)
      case _                 => None
    }
  }

}


/** Интерфейс ответа в рамках сервиса. */
trait IServiceAnswer extends IAnswer {

  /** Используемый сервис. */
  def service   : MExtService

}


case class ServiceAnswer(status: AnswerStatus, service: MExtService, replyTo: String, ctx2: JsCtx_t)
  extends IServiceAnswer

