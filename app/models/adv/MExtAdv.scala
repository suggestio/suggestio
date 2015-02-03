package models.adv

import io.suggest.model.{EsModelT, EsModelPlayJsonT, EsModelStaticT}
import io.suggest.util.SioEsUtil._
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.joda.time.DateTime
import play.api.libs.json.JsString
import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser, dateTimeParser, date2JsStr}

import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:32
 * Description: ES-Модель с инфой по одному размещению одной рекламной карточки на одном внешнем сервисе.
 */
object MExtAdv extends EsModelStaticT with PlayMacroLogsImpl {

  override type T = MExtAdv
  override val ES_TYPE_NAME = "ea"
  
  val AD_ID_ESFN          = "adId"
  val EXT_TARGET_ID_ESFN  = "etId"
  val URL_ESFN            = "url"
  val PERSON_ID_ESFN      = "personId"
  val DATE_ESFN           = "dt"

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(enabled = false)
    )
  }


  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(AD_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(EXT_TARGET_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(URL_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(PERSON_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldDate(DATE_ESFN, index = null, include_in_all = false)
    )
  }

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MExtAdv(
      id          = id,
      versionOpt  = version,
      adId        = stringParser(m(AD_ID_ESFN)),
      extTargetId = stringParser(m(EXT_TARGET_ID_ESFN)),
      url         = stringParser(m(URL_ESFN)),
      personId    = stringParser(m(PERSON_ID_ESFN)),
      date        = m.get(DATE_ESFN)
        .fold(DateTime.now)(dateTimeParser)
    )
  }

  def adIdQuery(adId: String) = QueryBuilders.termQuery(AD_ID_ESFN, adId)

  /**
   * Поиск по id рекламной карточки.
   * @param adId id рекламной карточки.
   * @return Фьючерс с последовательностью экземпляров модели в неопределённом порядке.
   */
  def findForAd(adId: String, limit: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
               (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery(adIdQuery(adId))
      .setSize(limit)
      .setFrom(offset)
      .execute()
      .map { searchResp2list }
  }

}


import MExtAdv._


case class MExtAdv(
  adId          : String,
  extTargetId   : String,
  url           : String,
  personId      : String,
  date          : DateTime = DateTime.now(),
  versionOpt    : Option[Long] = None,
  id            : Option[String] = None
) extends EsModelT with EsModelPlayJsonT {

  override type T = this.type
  override def companion = MExtAdv

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    List(
      AD_ID_ESFN            -> JsString(adId),
      EXT_TARGET_ID_ESFN    -> JsString(extTargetId),
      URL_ESFN              -> JsString(url),
      PERSON_ID_ESFN        -> JsString(personId),
      DATE_ESFN             -> date2JsStr(date)
    )
  }

}
