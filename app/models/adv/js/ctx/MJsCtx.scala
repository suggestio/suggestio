package models.adv.js.ctx

import io.suggest.adv.ext.model.ctx.MJsCtxFieldsT
import models.adv.JsExtTarget
import models.adv.ext.MExtServiceInfo
import models.adv.js.{AnswerStatus, MJsAction}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.01.15 17:45
  * Description: JSON-модель для обмена всеми важными данными между JS и актором на сервере.
  * Все команды между сервером и клиентом, а также данные состояния описываются для сервера этой вот структурой.
  *
  * Клиент и сервер "пасуют" друг другу данные в данном контексте,
  * одновременно передавая друг другу управление процессом.
  */
object MJsCtx extends MJsCtxFieldsT {

  private def _optSeqFlatmap[T](v: Option[Seq[T]]): Seq[T] = {
    v.getOrElse( Nil )
  }

  private def _optSeqContramap[T](col: Seq[T]): Option[Seq[T]] = {
    if (col.isEmpty) None else Some(col)
  }

  /** Поддержка сериализации/десериализации JSON. */
  implicit val FORMAT: OFormat[MJsCtx] = (
    (__ \ ACTION_FN).formatNullable[MJsAction] and
    (__ \ ADS_FN).formatNullable[Seq[MAdCtx]]
      .inmap(_optSeqFlatmap[MAdCtx], _optSeqContramap[MAdCtx]) and
    (__ \ TARGET_FN).formatNullable[JsExtTarget] and
    (__ \ DOMAIN_FN).formatNullable[Seq[String]]
      .inmap(_optSeqFlatmap[String], _optSeqContramap[String]) and
    (__ \ STATUS_FN).formatNullable[AnswerStatus] and
    (__ \ SERVICE_FN).formatNullable[MExtServiceInfo] and
    (__ \ ERROR_FN).formatNullable[JsErrorInfo] and
    (__ \ CUSTOM_FN).formatNullable[JsValue] and
    (__ \ SVC_TARGETS_FN).formatNullable[Seq[JsExtTarget]]
      .inmap(_optSeqFlatmap[JsExtTarget], _optSeqContramap[JsExtTarget])
  )(apply, unlift(unapply))

}


/**
  * Полноценный контекст, удобный для редактирования через copy().
  * @param action Текущее действие. Выставляется сервером, и возвращается назад клиентом.
  * @param mads Данные по рекламным карточкам.
  * @param target Данные по текущей цели.
  * @param domain Нормализованное доменное имя.
  * @param custom JsObject, содержащий остаточный context, которые не парсится сервером.
  * @param svcTargets Все цели сервиса. Содержит данные только при инициализации.
  * @param service Контейнер данных о текущем сервисе, раскрываемых для JS-стороны.
  */
case class MJsCtx(
  action    : Option[MJsAction],
  mads      : Seq[MAdCtx]               = Nil,
  target    : Option[JsExtTarget]       = None,
  domain    : Seq[String]               = Nil,
  status    : Option[AnswerStatus]      = None,
  service   : Option[MExtServiceInfo]   = None,
  error     : Option[JsErrorInfo]       = None,
  custom    : Option[JsValue]           = None,
  svcTargets: Seq[JsExtTarget]          = Nil
)

