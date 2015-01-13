package models.adv

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.{EsModelT, EsModelPlayJsonT, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import play.api.libs.json.{JsObject, JsString}
import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.stringParser

import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:01
 * Description: ES-модель описания одного сервиса-цели для внешнего размещения.
 * Он содержит целевую ссылку, id обнаруженного сервиса, дату добавление и прочее.
 */

object MExtTarget extends EsModelStaticT with PlayMacroLogsImpl {

  override type T = MExtTarget

  override val ES_TYPE_NAME: String = "aet"

  val URL_ESFN          = "url"
  val SERVICE_ID_ESFN   = "srv"
  val NAME_ESFN         = "name"
  val ADN_ID_ESFN       = "adnId"


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
      FieldString(ADN_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
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
      name        = m.get(NAME_ESFN).map(stringParser)
    )
  }


  def adnIdQuery(adnId: String) = QueryBuilders.termQuery(ADN_ID_ESFN, adnId)

  /**
   * Поиск для выбранного узла.
   * @param adnId id узла ADN.
   * @return Последовательность результатов в неопределённом порядке.
   */
  def findByAdnId(adnId: String, limit: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                 (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery(adnIdQuery(adnId))
      .setSize(limit)
      .setFrom(offset)
      .execute()
      .map { searchResp2list }
  }

}


import MExtTarget._


case class MExtTarget(
  url           : String,
  service       : MExtService,
  adnId         : String,
  name          : Option[String] = None,
  versionOpt    : Option[Long] = None,
  id            : Option[String] = None
) extends EsModelT with EsModelPlayJsonT with JsPublishTargetT {

  override type T = this.type
  override def companion = MExtTarget

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    SERVICE_ID_ESFN   -> JsString(service.strId) ::
    ADN_ID_ESFN       -> JsString(adnId) ::
    toJsTargetPlayJsonFields
  }

}


/** Для отправки в JS нужны объекты с простым интерфейсом. */
trait JsPublishTargetT {

  /** Ссылка на целевую страницу. */
  def url: String

  /** Опциональное название по мнению пользователя. */
  def name: Option[String]

  /** Генерация экземпляра play.json.JsObject на основе имеющихся данных. */
  def toJsTargetPlayJson: JsObject = JsObject(toJsTargetPlayJsonFields)

  /** Генерация JSON-тела на основе имеющихся данных. */
  def toJsTargetPlayJsonFields: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      URL_ESFN -> JsString(url)
    )
    val _name = name
    if (_name.isDefined)
      acc ::= NAME_ESFN -> JsString(_name.get)
    acc
  }
}

