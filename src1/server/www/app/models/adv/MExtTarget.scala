package models.adv

import java.time.OffsetDateTime

import io.suggest.adv.ext.model.ctx.MExtTargetT
import io.suggest.es.MappingDsl
import play.api.libs.json._
import play.api.libs.functional.syntax._
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.es.search.EsDynSearchStatic
import io.suggest.ext.svc.MExtService
import io.suggest.util.logs.MacroLogsImpl
import models.adv.ext.MExtTargetSearchArgs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:01
 * Description: ES-модель описания одного сервиса-цели для внешнего размещения.
 * Он содержит целевую ссылку, id обнаруженного сервиса, дату добавление и прочее.
 */

// TODO Модель следует объеденить с MNode. JsExtTarget вынести в отдельную модель.

object MExtTargetFields {

  /** Имя поле со ссылкой на цель. */
  def URL_ESFN          = "url"
  /** Имя поля, в котором хранится id внешнего сервиса, к которому относится эта цель. */
  def SERVICE_ID_ESFN   = "srv"
  /** Имя поля с названием этой цели. */
  def NAME_ESFN         = "name"
  /** Имя поля с id узла, к которому привязан данный интанс. */
  def ADN_ID_ESFN       = "adnId"
  /** Поле даты создания. */
  def DATE_CREATED_ESFN = "dci"

}


@Singleton
class MExtTargets
  extends EsModelStatic
  with MacroLogsImpl
  with EsDynSearchStatic[MExtTargetSearchArgs]
  with EsmV2Deserializer
  with EsModelJsonWrites
{

  override type T = MExtTarget

  override def ES_TYPE_NAME: String = "aet"
  override def ES_INDEX_NAME = ???

  /** Сборка маппинга индекса по новому формату. */
  override def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping = {
    import dsl._
    val F = MExtTargetFields
    IndexMapping(
      source = Some( FSource(enabled = someTrue) ),
      properties = Some {
        Json.obj(
          F.URL_ESFN -> FText.indexedJs,
          F.SERVICE_ID_ESFN -> FKeyWord.indexedJs,
          F.NAME_ESFN -> FText.indexedJs,
          F.ADN_ID_ESFN -> FKeyWord.indexedJs,
          // Для сортировке по дате требуется индексация.
          F.DATE_CREATED_ESFN -> FDate.indexedJs,
        )
      }
    )
  }


  /** Кешируем почти собранный десериализатор. */
  private def DATA_FORMAT: OFormat[MExtTarget] = {
    val F = MExtTargetFields
    (
      (__ \ F.URL_ESFN).format[String] and
      (__ \ F.SERVICE_ID_ESFN).format[MExtService] and
      (__ \ F.ADN_ID_ESFN).format[String] and
      (__ \ F.NAME_ESFN).formatNullable[String] and
      (__ \ F.DATE_CREATED_ESFN).format[OffsetDateTime]
    )(
      (url, service, adnId, name, dataCreated) =>
        MExtTarget(url, service, adnId, name, dataCreated),
      et => (et.url, et.service, et.adnId, et.name, et.dateCreated)
    )
  }

  /** JSON deserializer. */
  override protected def esDocReads(meta: EsDocMeta): Reads[T] = {
    DATA_FORMAT.map( withDocMeta(_, meta) )
  }

  override def esDocWrites = DATA_FORMAT

  override def withDocMeta(m: MExtTarget, docMeta: EsDocMeta): MExtTarget = {
    m.copy(
      id          = docMeta.id,
      versioning  = docMeta.version,
    )
  }

}


case class MExtTarget(
                       override val url          : String,
                       service                   : MExtService,
                       adnId                     : String,
                       override val name         : Option[String]    = None,
                       dateCreated               : OffsetDateTime    = OffsetDateTime.now,
                       override val versioning   : EsDocVersion      = EsDocVersion.empty,
                       override val id           : Option[String]    = None,
)
  extends EsModelT
  with IExtTarget


// Поддержка JMX для ES-модели.
trait MExtTargetsJmxMBean extends EsModelJMXMBeanI
final class MExtTargetsJmx @Inject()(
                                      override val companion      : MExtTargets,
                                      override val esModelJmxDi   : EsModelJmxDi,
                                    )
  extends EsModelJMXBaseImpl
  with MExtTargetsJmxMBean
{
  override type X = MExtTarget
}


/** Упрощенный интерфейс MExtTarget для js target-таргетирования. */
trait IExtTarget {

  /** Ссылка на целевую страницу. */
  def url: String

  /** Опциональное название по мнению пользователя. */
  def name: Option[String]

}

/** Враппер над [[IExtTarget]]. */
trait IExtTargetWrapper extends IExtTarget {
  def _targetUnderlying: IExtTarget
  override def url = _targetUnderlying.url
  override def name = _targetUnderlying.name
}


object JsExtTarget extends MExtTargetT {

  implicit def FORMAT: OFormat[JsExtTarget] = (
    (__ \ ID_FN).format[String] and
    (__ \ URL_FN).format[String] and
    (__ \ ON_CLICK_URL_FN).format[String] and
    (__ \ NAME_FN).formatNullable[String] and
    (__ \ CUSTOM_FN).formatNullable[JsValue]
  )(apply _, unlift(unapply))

}

/** Модель того, что лежит в ext adv js ctx._target. */
case class JsExtTarget(
  id          : String,
  url         : String,
  onClickUrl  : String,
  name        : Option[String] = None,
  custom      : Option[JsValue] = None
) extends IExtTarget

