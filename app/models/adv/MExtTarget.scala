package models.adv

import io.suggest.adv.ext.model.ctx.MExtTargetT
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model._
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import io.suggest.ym.model.common.EsDynSearchStatic
import models.adv.search.etg.IExtTargetSearchArgs
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.model.EsModel.stringParser

import scala.collection.Map

import java.{util => ju}

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:01
 * Description: ES-модель описания одного сервиса-цели для внешнего размещения.
 * Он содержит целевую ссылку, id обнаруженного сервиса, дату добавление и прочее.
 */

object MExtTarget extends EsModelStaticT with PlayMacroLogsImpl with EsDynSearchStatic[IExtTargetSearchArgs] {

  override type T = MExtTarget

  override val ES_TYPE_NAME: String = "aet"

  /** Имя поле со ссылкой на цель. */
  val URL_ESFN          = "url"
  /** Имя поля, в котором хранится id внешнего сервиса, к которому относится эта цель. */
  val SERVICE_ID_ESFN   = "srv"
  /** Имя поля с названием этой цели. */
  val NAME_ESFN         = "name"
  /** Имя поля с id узла, к которому привязан данный интанс. */
  val ADN_ID_ESFN       = "adnId"
  /** В поле с этим именем хранятся контекстные данные, заданные js'ом. */
  val CTX_DATA_ESFN     = "stored"

  /** Поле даты создания. Было добавлено только 2014.mar.05, из-за необходимости сортировки. */
  val DATE_CREATED_ESFN = "dc"


  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(URL_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(SERVICE_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(ADN_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(CTX_DATA_ESFN, enabled = false, properties = Nil),
      FieldDate(DATE_CREATED_ESFN, index = null, include_in_all = false)
    )
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MExtTarget(
      id          = id,
      versionOpt  = version,
      url         = stringParser(m(URL_ESFN)),
      service     = MExtServices.withName( stringParser(m(SERVICE_ID_ESFN)) ),
      adnId       = stringParser(m(ADN_ID_ESFN)),
      name        = m.get(NAME_ESFN)
        .map(stringParser),
      dateCreated = m.get(DATE_CREATED_ESFN)
        .fold(DateTime.now)(EsModel.dateTimeParser),
      ctxData     = m.get(CTX_DATA_ESFN).flatMap {
        case jm: ju.Map[_, _] =>
          if (jm.isEmpty) {
            None
          } else {
            val rawJson = JacksonWrapper.serialize(jm)
            val jso = Json.parse(rawJson).asInstanceOf[JsObject]
            Some(jso)
          }
        case _ => None
      }
    )
  }

}


import MExtTarget._


case class MExtTarget(
  url           : String,
  service       : MExtService,
  adnId         : String,
  name          : Option[String]    = None,
  dateCreated   : DateTime          = DateTime.now,
  ctxData       : Option[JsObject]  = None,
  versionOpt    : Option[Long]      = None,
  id            : Option[String]    = None
) extends EsModelT with EsModelPlayJsonT with IExtTarget {

  override type T = this.type
  override def companion = MExtTarget

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    SERVICE_ID_ESFN   -> JsString(service.strId) ::
    ADN_ID_ESFN       -> JsString(adnId) ::
    DATE_CREATED_ESFN -> EsModel.date2JsStr(dateCreated) ::
    toJsTargetPlayJsonFields
  }

}


// Поддержка JMX для ES-модели.
trait MExtTargetJmxMBean extends EsModelJMXMBeanI
final class MExtTargetJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MExtTargetJmxMBean
{
  override def companion = MExtTarget
}


/** Упрощенный интерфейс MExtTarget для js target-таргетирования. */
trait IExtTarget {

  /** Ссылка на целевую страницу. */
  def url: String

  /** Опциональное название по мнению пользователя. */
  def name: Option[String]

  /** Произвольные данные контекста, заданные на стороне js. */
  def ctxData: Option[JsObject]

  /** Генерация экземпляра play.json.JsObject на основе имеющихся данных. */
  def toJsTargetPlayJson: JsObject = JsObject(toJsTargetPlayJsonFields)

  /** Генерация JSON-тела на основе имеющихся данных. */
  def toJsTargetPlayJsonFields: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      URL_ESFN            -> JsString(url)
    )
    // name
    val _name = name
    if (_name.isDefined)
      acc ::= NAME_ESFN -> JsString(_name.get)
    // ctxData
    val _ctxData = ctxData
    if (_ctxData.nonEmpty)
      acc ::= CTX_DATA_ESFN -> _ctxData.get
    // результат
    acc
  }

}

/** Враппер над [[IExtTarget]]. */
trait IExtTargetWrapper extends IExtTarget {
  def _targetUnderlying: IExtTarget
  override def url = _targetUnderlying.url
  override def ctxData = _targetUnderlying.ctxData
  override def name = _targetUnderlying.name
}


object JsExtTarget extends MExtTargetT {

  /** mapper из JSON. */
  implicit def reads: Reads[JsExtTarget] = (
    (__ \ URL_FN).read[String] and
    (__ \ ON_CLICK_URL_FN).read[String] and
    (__ \ NAME_FN).readNullable[String]
  )(apply(_, _, _))

  /** unmapper в JSON. */
  implicit def writes: Writes[JsExtTarget] = (
    (__ \ URL_FN).write[String] and
    (__ \ ON_CLICK_URL_FN).write[String] and
    (__ \ NAME_FN).writeNullable[String]
  )(unlift(unapply))

  
  def apply(target: IExtTarget, onClickUrl: String): JsExtTarget = {
    apply(
      url         = target.url,
      onClickUrl  = onClickUrl,
      name        = target.name
    )
  }

}

/** Модель того, что лежит в ext adv js ctx._target. */
case class JsExtTarget(
  url         : String,
  onClickUrl  : String,
  name        : Option[String] = None
) extends IExtTarget {

  // TODO Произвольные данные контекста, заданные на стороне js.
  override def ctxData: Option[JsObject] = None

}

