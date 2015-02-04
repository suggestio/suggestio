package models.adv.js.ctx

import io.suggest.model.EsModel.FieldsJsonAcc
import models.adv.{MExtServices, MExtService, JsExtTarget}
import models.adv.js.{AnswerStatuses, AnswerStatus}
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
object MJsCtx {

  val ADS_FN      = "_ads"
  val TARGET_FN   = "_target"
  val STATUS_FN   = "_status"
  val DOMAIN_FN   = "_domain"
  val SERVICE_FN  = "_service"
  val ERROR_FN    = "_error"

  /** Все поля, которые поддерживает контекст. Обычно -- все вышеперечисленные поля. */
  val FIELDS = Set(ADS_FN, TARGET_FN, STATUS_FN, DOMAIN_FN)

  /** Извлекатель контекста из JSON. Т.к. в json могут быть посторонние для сервера данные, нужно
    * парсить контекст аккуратно. */
  implicit def reads = new Reads[MJsCtx] {
    override def reads(json: JsValue): JsResult[MJsCtx] = {
      try {
        val ctx = MJsCtx(
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
            val fs = json.asInstanceOf[JsObject]
              .fields
              .filter { case (k, _) => !(FIELDS contains k)}
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
        val fields1 =  o.restCtx.fields
          .iterator
          .filter { case (fn, _) => !(FIELDS contains fn) }
          .foldLeft(acc) { (acc1, f) => f :: acc1 }
        JsObject(fields1)
      }
    }
  }

}


/**
 * Полноценный контекст, удобный для редактирования через copy().
 * @param mads Данные по рекламным карточкам.
 * @param target Данные по текущей цели.
 * @param domain Нормализованное доменное имя.
 * @param restCtx JsObject, содержащий остаточный context, которые не парсится сервером.
 */
case class MJsCtx(
  mads      : Seq[MAdCtx]           = Seq.empty,
  target    : Option[JsExtTarget]   = None,
  domain    : Seq[String]           = Seq.empty,
  status    : Option[AnswerStatus]  = None,
  service   : Option[MExtService]   = None,
  error     : Option[JsErrorInfo]   = None,
  restCtx   : JsObject              = JsObject(Nil)
)

