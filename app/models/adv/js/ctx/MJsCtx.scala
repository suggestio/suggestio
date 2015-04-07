package models.adv.js.ctx

import io.suggest.adv.ext.model.ctx.MJsCtxFieldsT
import models.adv.{MExtServices, MExtService, JsExtTarget}
import models.adv.js.{MJsAction, AnswerStatus}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.01.15 17:45
 * Description: Модель-враппер над json для быстрого прозрачного доступа к служебным данным.
 * Динамические части моделей существуют как в виде json-обёрток, так и в виде нормальных моделей.
 * Это сделано, т.к. далеко не всегда нужно что-то парсить и менять в контексте, а полный парсинг контекста
 * в будущем может стать ресурсоёмким процессом.
 */
object MJsCtx extends MJsCtxFieldsT {

  implicit def reads: Reads[MJsCtx] = {
    def optSeqFlatmap[T](v: Option[Seq[T]]): Seq[T] = v getOrElse Nil
    (
      (__ \ ACTION_FN).readNullable[MJsAction] and
      (__ \ ADS_FN).readNullable[Seq[MAdCtx]].map(optSeqFlatmap[MAdCtx]) and
      (__ \ TARGET_FN).readNullable[JsExtTarget] and
      (__ \ DOMAIN_FN).readNullable[Seq[String]].map(optSeqFlatmap[String]) and
      (__ \ STATUS_FN).readNullable[AnswerStatus] and
      (__ \ SERVICE_FN).readNullable[MExtService] and
      (__ \ ERROR_FN).readNullable[JsErrorInfo] and
      (__ \ CUSTOM_FN).readNullable[JsValue] and
      (__ \ SVC_TARGETS_FN).readNullable[Seq[JsExtTarget]].map(optSeqFlatmap[JsExtTarget])
    )(apply _)
  }

  implicit def writes2: Writes[MJsCtx] = {
    def optSeqContramap[T](col: Seq[T]): Option[Seq[T]] = if (col.isEmpty) None else Some(col)
    (
      (__ \ ACTION_FN).writeNullable[MJsAction] and
      (__ \ ADS_FN).writeNullable[Seq[MAdCtx]].contramap(optSeqContramap[MAdCtx]) and
      (__ \ TARGET_FN).writeNullable[JsExtTarget] and
      (__ \ DOMAIN_FN).writeNullable[Seq[String]].contramap(optSeqContramap[String]) and
      (__ \ STATUS_FN).writeNullable[AnswerStatus] and
      (__ \ SERVICE_FN).writeNullable(MExtServices.writes) and
      (__ \ ERROR_FN).writeNullable[JsErrorInfo] and
      (__ \ CUSTOM_FN).writeNullable[JsValue] and
      (__ \ SVC_TARGETS_FN).writeNullable[Seq[JsExtTarget]].contramap(optSeqContramap[JsExtTarget])
    )(unlift(unapply))
  }

}


/**
 * Полноценный контекст, удобный для редактирования через copy().
 * @param action Текущее действие. Выставляется сервером, и возвращается назад клиентом.
 * @param mads Данные по рекламным карточкам.
 * @param target Данные по текущей цели.
 * @param domain Нормализованное доменное имя.
 * @param custom JsObject, содержащий остаточный context, которые не парсится сервером.
 * @param svcTargets Все цели сервиса. Содержит данные только при инициализации.
 */
case class MJsCtx(
  action    : Option[MJsAction],
  mads      : Seq[MAdCtx]           = Nil,
  target    : Option[JsExtTarget]   = None,
  domain    : Seq[String]           = Nil,
  status    : Option[AnswerStatus]  = None,
  service   : Option[MExtService]   = None,
  error     : Option[JsErrorInfo]   = None,
  custom    : Option[JsValue]       = None,
  svcTargets: Seq[JsExtTarget]      = Nil
)

