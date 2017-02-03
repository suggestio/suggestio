package models.adv

import java.time.OffsetDateTime

import io.suggest.adv.ext.model.ctx.MExtTargetT
import io.suggest.model.es._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.util.SioEsUtil._
import models.adv.search.etg.IExtTargetSearchArgs
import models.mext.{MExtService, MExtServices}
import play.api.i18n.Messages
import util.PlayMacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.util.JacksonParsing
import com.google.inject.{Inject, Singleton}
import io.suggest.es.search.EsDynSearchStatic
import models.mproj.ICommonDi

import scala.collection.Map
import scala.concurrent.ExecutionContext

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
  val URL_ESFN          = "url"
  /** Имя поля, в котором хранится id внешнего сервиса, к которому относится эта цель. */
  val SERVICE_ID_ESFN   = "srv"
  /** Имя поля с названием этой цели. */
  val NAME_ESFN         = "name"
  /** Имя поля с id узла, к которому привязан данный интанс. */
  val ADN_ID_ESFN       = "adnId"
  /** Поле даты создания. */
  val DATE_CREATED_ESFN = "dci"

}


@Singleton
class MExtTargets @Inject() (
  override val mCommonDi: ICommonDi
)
  extends EsModelStatic
  with PlayMacroLogsImpl
  with EsDynSearchStatic[IExtTargetSearchArgs]
  with EsmV2Deserializer
  with EsModelPlayJsonStaticT
{

  override type T = MExtTarget

  override def ES_TYPE_NAME: String = "aet"


  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(enabled = true)
    )
  }


  import MExtTargetFields._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(URL_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(SERVICE_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(ADN_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      // Для сортировке по дате требуется индексация.
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false)
    )
  }

  @deprecated("Delete it, replaced by deserializeOne2().", "2015.sep.07")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MExtTarget(
      id          = id,
      versionOpt  = version,
      url         = JacksonParsing.stringParser(m(URL_ESFN)),
      service     = MExtServices.withName( JacksonParsing.stringParser(m(SERVICE_ID_ESFN)) ),
      adnId       = JacksonParsing.stringParser(m(ADN_ID_ESFN)),
      name        = m.get(NAME_ESFN)
        .map( JacksonParsing.stringParser ),
      dateCreated = m.get(DATE_CREATED_ESFN)
        .fold(OffsetDateTime.now)(JacksonParsing.dateTimeParser)
    )
  }

  /** Кешируем почти собранный десериализатор. */
  private val _reads0 = {
    (__ \ URL_ESFN).read[String] and
    (__ \ SERVICE_ID_ESFN).read[MExtService] and
    (__ \ ADN_ID_ESFN).read[String] and
    (__ \ NAME_ESFN).readNullable[String] and
    (__ \ DATE_CREATED_ESFN).read[OffsetDateTime]
  }

  /** JSON deserializer. */
  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    _reads0 {
      (url, service, adnId, name, dataCreated) =>
        MExtTarget(url, service, adnId, name, dataCreated, meta.version, meta.id)
    }
  }


  /**
   * Создавать ли экземпляр этой модели для новых узлов?
   *
   * @param svc Сервис.
   * @param adnId id узла.
   * @param messages язык. Для связи с Messages().
   * @return Some с экземпляром [[MExtTarget]].
   *         None, если по дефолту таргет создавать не надо.
   */
  def dfltTarget(svc: MExtService, adnId: String)(implicit messages: Messages): Option[MExtTarget] = {
    svc.dfltTargetUrl.map { url =>
      MExtTarget(
        url     = url,
        adnId   = adnId,
        service = svc,
        name    = Some( messages(svc.iAtServiceI18N) )
      )
    }
  }

  override def writeJsonFields(m: T, acc: FieldsJsonAcc): FieldsJsonAcc = {
    import m._
    SERVICE_ID_ESFN   -> JsString(service.strId) ::
    ADN_ID_ESFN       -> JsString(adnId) ::
    DATE_CREATED_ESFN -> JacksonParsing.date2JsStr(dateCreated) ::
    toJsTargetPlayJsonFields
  }

}

/** Интерфейс для инжектируемых полей [[MExtTargets]]. */
trait IMExtTargets {
  def mExtTargets: MExtTargets
}


case class MExtTarget(
  url           : String,
  service       : MExtService,
  adnId         : String,
  name          : Option[String]    = None,
  dateCreated   : OffsetDateTime    = OffsetDateTime.now,
  versionOpt    : Option[Long]      = None,
  id            : Option[String]    = None
)
  extends EsModelT
    with IExtTarget


// Поддержка JMX для ES-модели.
trait MExtTargetsJmxMBean extends EsModelJMXMBeanI
final class MExtTargetsJmx @Inject()(
  override val companion  : MExtTargets,
  override val ec         : ExecutionContext
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

  /** Генерация экземпляра play.json.JsObject на основе имеющихся данных. */
  def toJsTargetPlayJson: JsObject = JsObject(toJsTargetPlayJsonFields)

  /** Генерация JSON-тела на основе имеющихся данных. */
  def toJsTargetPlayJsonFields: FieldsJsonAcc = {
    import MExtTargetFields._
    var acc: FieldsJsonAcc = List(
      URL_ESFN            -> JsString(url)
    )
    // name
    val _name = name
    if (_name.isDefined)
      acc ::= NAME_ESFN -> JsString(_name.get)
    // результат
    acc
  }

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

