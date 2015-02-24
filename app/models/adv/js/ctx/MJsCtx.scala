package models.adv.js.ctx

import io.suggest.adv.ext.model.ctx.MJsCtxFieldsT
import io.suggest.model.EsModel.FieldsJsonAcc
import models.adv.{MExtServices, MExtService, JsExtTarget}
import models.adv.js.{MJsAction, AnswerStatus}
import play.api.libs.json._

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

  override val ACTION_FN  = super.ACTION_FN
  override val ADS_FN     = super.ADS_FN
  override val TARGET_FN  = super.TARGET_FN
  override val STATUS_FN  = super.STATUS_FN
  override val DOMAIN_FN  = super.DOMAIN_FN
  override val SERVICE_FN = super.SERVICE_FN
  override val ERROR_FN   = super.ERROR_FN

  /** Все поля, которые поддерживает контекст. Обычно -- все вышеперечисленные поля. */
  def ALL_FIELDS = Set(ACTION_FN, ADS_FN, TARGET_FN, STATUS_FN, DOMAIN_FN, SERVICE_FN, ERROR_FN)

  /** Извлекатель контекста из JSON. Т.к. в json могут быть посторонние для сервера данные, нужно
    * парсить контекст аккуратно. */
  implicit def reads = new Reads[MJsCtx] {
    override def reads(json: JsValue): JsResult[MJsCtx] = {
      try {
        val ctx = MJsCtx(
          action = (json \ ACTION_FN)
            .asOpt[MJsAction],
          mads = (json \ ADS_FN)
            .asOpt[Seq[MAdCtx]]
            .getOrElse(Seq.empty),
          target = (json \ TARGET_FN)
            .asOpt[JsExtTarget],
          status = (json \ STATUS_FN)
            .asOpt[AnswerStatus],
          domain = (json \ DOMAIN_FN)
            .asOpt[Seq[String]]
            .getOrElse(Seq.empty),
          service = (json \ SERVICE_FN)
            .asOpt[MExtService],
          error = (json \ ERROR_FN)
            .asOpt[JsErrorInfo],
          restCtx = {
            lazy val allFields = ALL_FIELDS
            val fs = json.asInstanceOf[JsObject]
              .fields
              .filter { case (k, _) => !(allFields contains k)}
            JsObject(fs)
          }
        )
        JsSuccess(ctx)
      } catch {
        case ex: Exception => JsError("cannot parse ctx")
      }
    }
  }

  /** Генератор JSON из экземпляра MJsCtx. */
  implicit def writes = new Writes[MJsCtx] {
    override def writes(o: MJsCtx): JsValue = {
      var acc: FieldsJsonAcc = Nil
      if (o.action.nonEmpty)
        acc ::= ACTION_FN -> Json.toJson(o.action.get)
      if (o.mads.nonEmpty)
        acc ::= ADS_FN -> Json.toJson(o.mads)
      if (o.target.nonEmpty)
        acc ::= TARGET_FN -> Json.toJson(o.target.get)
      if (o.status.nonEmpty)
        acc ::= STATUS_FN -> Json.toJson(o.status.get)
      if (o.domain.nonEmpty)
        acc ::= DOMAIN_FN -> Json.toJson(o.domain)
      if (o.service.nonEmpty)
        acc ::= SERVICE_FN -> Json.toJson(o.service.get)(MExtServices.writes)
      if (o.error.nonEmpty)
        acc ::= ERROR_FN -> Json.toJson(o.error.get)
      // Объединение результата с restJson, если требуется.
      if (acc.isEmpty) {
        o.restCtx
      } else if (o.restCtx.fields.isEmpty) {
        JsObject(acc)
      } else {
        // Залить левые поля из restCtx в финальный acc.
        lazy val allFields = ALL_FIELDS
        val fields1 =  o.restCtx.fields
          .iterator
          .filter { case (fn, _) => !(allFields contains fn) }
          .foldLeft(acc) { (acc1, f) => f :: acc1 }
        JsObject(fields1)
      }
    }
  }

}


/**
 * Полноценный контекст, удобный для редактирования через copy().
 * @param action Текущее действие. Выставляется сервером, и возвращается назад клиентом.
 * @param mads Данные по рекламным карточкам.
 * @param target Данные по текущей цели.
 * @param domain Нормализованное доменное имя.
 * @param restCtx JsObject, содержащий остаточный context, которые не парсится сервером.
 */
case class MJsCtx(
  action    : Option[MJsAction],
  mads      : Seq[MAdCtx]           = Seq.empty,
  target    : Option[JsExtTarget]   = None,
  domain    : Seq[String]           = Seq.empty,
  status    : Option[AnswerStatus]  = None,
  service   : Option[MExtService]   = None,
  error     : Option[JsErrorInfo]   = None,
  restCtx   : JsObject              = JsObject(Nil)
)

