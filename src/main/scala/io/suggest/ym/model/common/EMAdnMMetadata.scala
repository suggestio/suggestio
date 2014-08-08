package io.suggest.ym.model.common

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.ym.model.MWelcomeAd
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.joda.time.DateTime
import io.suggest.model.{EsModel, EsModelT, EsModelStaticT}
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._, FieldIndexingVariants.FieldIndexingVariant
import play.api.libs.json._
import scala.collection.JavaConversions._
import scala.concurrent.{Future, ExecutionContext}
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 13:43
 * Description: Метаданные узлов-участников рекламной сети: названия, адреса, даты и т.д.
 */

object EMAdnMMetadataStatic {

  val META_ESFN         = "meta"

  def META_FLOOR_ESFN           = META_ESFN + "." + AdnMMetadata.FLOOR_ESFN
  def META_WELCOME_AD_ID_ESFN   = META_ESFN + "." + AdnMMetadata.WELCOME_AD_ID

  /**
   * Собрать указанные значения строковых полей в аккамулятор-множество.
   * @param searchResp Экземпляр searchResponse.
   * @param fn Название поля, значение которого собираем в акк.
   * @param acc0 Начальный акк.
   * @param keepAliveMs keepAlive для курсоров на стороне сервера ES в миллисекундах.
   * @return Фьчерс с результирующим аккамулятором-множеством.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html#scrolling]]
   */
  def searchScrollResp2strSet(searchResp: SearchResponse, fn: String, firstReq: Boolean, acc0: Set[String] = Set.empty, keepAliveMs: Long = 60000L)
                             (implicit ec: ExecutionContext, client: Client): Future[Set[String]] = {
    val hits = searchResp.getHits.getHits
    if (!firstReq && hits.length == 0) {
      Future successful acc0
    } else {
      // Запустить в фоне получение следующей порции результатов
      val nextScrollRespFut = client.prepareSearchScroll(searchResp.getScrollId)
        .setScroll(new TimeValue(keepAliveMs))
        .execute()
      // Синхронно залить результаты текущего реквеста в аккамулятор
      val accNew = hits.foldLeft[List[String]] (Nil) { (acc, hit) =>
        hit.field(fn) match {
          case null =>
            acc
          case values =>
            values.getValues.foldLeft (acc) {
              (acc1, v)  =>  v.toString :: acc1
            }
        }
      }
      val acc1 = acc0 ++ accNew
      // Асинхронно перейти на следующую итерацию, дождавшись новой порции результатов.
      nextScrollRespFut flatMap { searchResp2 =>
        searchScrollResp2strSet(searchResp2, fn, firstReq = false, acc1, keepAliveMs)
      }
    }
  }

}

import EMAdnMMetadataStatic._


trait EMAdnMMetadataStatic extends EsModelStaticT {

  override type T <: EMAdnMMetadata

  abstract override def generateMappingProps: List[DocField] = {
    val f = FieldObject(META_ESFN, enabled = true, properties = AdnMMetadata.generateMappingProps)
    f :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (META_ESFN, value) =>
        acc.meta = AdnMMetadata.deserialize(value)
    }
  }

  /** Собрать все id рекламных карточек, которые встречаются во всех документах модели.
    * Внутри используется match_all query + scroll.
    * @return Множество всех значений welcomeAdId.
    */
  def findAllWelcomeAdIds(maxResultsPerStep: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[Set[String]] = {
    val fn = META_WELCOME_AD_ID_ESFN
    prepareScroll()
      .setQuery( QueryBuilders.matchAllQuery() )
      .setSize(maxResultsPerStep)
      .addField(fn)
      .execute()
      .flatMap { searchResp =>
        searchScrollResp2strSet(searchResp, fn, firstReq = true, keepAliveMs = SCROLL_KEEPALIVE_MS_DFLT)
      }
  }

}


trait EMAdnMMetadata extends EsModelT {
  override type T <: EMAdnMMetadata

  var meta: AdnMMetadata

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val f = META_ESFN -> meta.toPlayJson
    f :: super.writeJsonFields(acc)
  }

  override def eraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val fut = super.eraseResources
    // Раньше вызывался MWelcomeAd.deleteByProducerId1by1(producerId) через событие SioNotifier.
    meta.welcomeAdId.fold(fut) { welcomeAdId =>
      MWelcomeAd.getById(welcomeAdId)
        .flatMap {
          case Some(mwa) =>
            mwa.eraseResources
              .flatMap { _ => mwa.delete }
          case None => Future successful false
        } flatMap {
          _ => fut
        }
    }
  }
}


object AdnMMetadata {

  val NAME_ESFN               = "name"
  val TOWN_ESFN               = "town"
  val ADDRESS_ESFN            = "address"
  val DATE_CREATED_ESFN       = "dateCreated"
  val PHONE_ESFN              = "phone"
  val SITE_URL_ESFN           = "siteUrl"
  val DESCRIPTION_ESFN        = "description"
  val AUDIENCE_DESCR_ESFN     = "audDescr"
  val HUMAN_TRAFFIC_AVG_ESFN  = "htAvg"
  val INFO_ESFN               = "info"
  val COLOR_ESFN              = "color"
  val WELCOME_AD_ID           = "welcomeAdId"
  val FLOOR_ESFN              = "floor"
  val SECTION_ESFN            = "section"
  val LOCATION_ESFN           = "location"


  private def fieldString(fn: String, iia: Boolean = true, index: FieldIndexingVariant = FieldIndexingVariants.no) = {
    FieldString(fn, include_in_all = iia, index = FieldIndexingVariants.no)
  }

  def generateMappingProps: List[DocField] = List(
    fieldString(NAME_ESFN, iia = true),
    fieldString(DESCRIPTION_ESFN, iia = true),
    FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    // legal
    fieldString(TOWN_ESFN, iia = true),
    fieldString(ADDRESS_ESFN, iia = true),
    fieldString(PHONE_ESFN, iia = true),
    fieldString(FLOOR_ESFN, iia = true, index = FieldIndexingVariants.not_analyzed),   // Внезапно, вдруг кто-то захочет найти все магазины на первом этаже.
    fieldString(SECTION_ESFN, iia = true),
    fieldString(SITE_URL_ESFN),
    // 2014.06.30: Рекламные характеристики узла.
    fieldString(AUDIENCE_DESCR_ESFN),
    FieldNumber(HUMAN_TRAFFIC_AVG_ESFN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.analyzed, include_in_all = false),
    FieldString(INFO_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    // Перемещено из visual - TODO Перенести в conf.
    FieldString(COLOR_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(WELCOME_AD_ID, index = FieldIndexingVariants.no, include_in_all = false)
  )

  /** Десериализация сериализованного экземпляра класса AdnMMetadata. */
  val deserialize: PartialFunction[Any, AdnMMetadata] = {
    case jmap: ju.Map[_,_] =>
      import EsModel.stringParser
      AdnMMetadata(
        name        = stringParser(jmap get NAME_ESFN),
        description = Option(jmap get DESCRIPTION_ESFN) map stringParser,
        dateCreated = EsModel.dateTimeParser(jmap get DATE_CREATED_ESFN),
        town        = Option(jmap get TOWN_ESFN) map stringParser,
        address     = Option(jmap get ADDRESS_ESFN) map stringParser,
        phone       = Option(jmap get PHONE_ESFN) map stringParser,
        floor       = Option(jmap get FLOOR_ESFN) map stringParser,
        section     = Option(jmap get SECTION_ESFN) map stringParser,
        siteUrl     = Option(jmap get SITE_URL_ESFN) map stringParser,
        audienceDescr = Option(jmap get AUDIENCE_DESCR_ESFN) map stringParser,
        humanTrafficAvg = Option(jmap get HUMAN_TRAFFIC_AVG_ESFN) map EsModel.intParser,
        info        = Option(jmap get INFO_ESFN) map stringParser,
        color       = Option(jmap get COLOR_ESFN) map stringParser,
        welcomeAdId = Option(jmap get WELCOME_AD_ID) map stringParser,
        location    = Option(jmap get LOCATION_ESFN) flatMap GeoPoint.deserializeOpt
      )
  }
}


/**
 * Экземпляр контейнера метаданных узла.
 * @param name Отображаемое имя/название.
 * @param description Пользовательское описание.
 * @param town Город.
 * @param address Адрес в городе.
 * @param phone Телефонный номер.
 * @param floor Этаж.
 * @param section Номер секции/павильона/кабинета/помещения и т.д.
 * @param siteUrl Ссылка на сайт.@param dateCreated
 * @param color Предпочтительный цвет оформления.
 * @param welcomeAdId id карточки приветствия в [[io.suggest.ym.model.MWelcomeAd]].
 */
case class AdnMMetadata(
  name          : String,
  description   : Option[String] = None,
  dateCreated   : DateTime = DateTime.now,
  // перемещено из legal
  town          : Option[String] = None,
  address       : Option[String] = None,
  phone         : Option[String] = None,
  floor         : Option[String] = None,
  section       : Option[String] = None,
  siteUrl       : Option[String] = None,
  // 2014.06.30: Рекламные характеристики узла-producer'а.
  audienceDescr : Option[String] = None,
  humanTrafficAvg: Option[Int]   = None,
  info          : Option[String] = None,
  location      : Option[GeoPoint] = None,
  // перемещено из visual
  color         : Option[String] = None,
  welcomeAdId   : Option[String] = None   // TODO Перенести в поле MAdnNode.conf.welcomeAdId
) {
  import AdnMMetadata._

  /** Статически-типизированный json-генератор. */
  @JsonIgnore
  def toPlayJson: JsObject = {
    var acc0: FieldsJsonAcc = List(
      NAME_ESFN -> JsString(name),
      DATE_CREATED_ESFN -> EsModel.date2JsStr(dateCreated)
    )
    if (description.isDefined)
      acc0 ::= DESCRIPTION_ESFN -> JsString(description.get)
    if (town.isDefined)
      acc0 ::= TOWN_ESFN -> JsString(town.get)
    if (address.isDefined)
      acc0 ::= ADDRESS_ESFN -> JsString(address.get)
    if (phone.isDefined)
      acc0 ::= PHONE_ESFN -> JsString(phone.get)
    if (floor.isDefined)
      acc0 ::= FLOOR_ESFN -> JsString(floor.get)
    if (section.isDefined)
      acc0 ::= SECTION_ESFN -> JsString(section.get)
    if (siteUrl.isDefined)
      acc0 ::= SITE_URL_ESFN -> JsString(siteUrl.get)
    // 2014.06.30
    if (audienceDescr.isDefined)
      acc0 ::= AUDIENCE_DESCR_ESFN -> JsString(audienceDescr.get)
    if (humanTrafficAvg.isDefined)
      acc0 ::= HUMAN_TRAFFIC_AVG_ESFN -> JsNumber(humanTrafficAvg.get)
    if (info.isDefined)
      acc0 ::= INFO_ESFN -> JsString(info.get)
    // TODO Надобно переместить это в conf отсюда:
    if (color.isDefined)
      acc0 ::= COLOR_ESFN -> JsString(color.get)
    if (welcomeAdId.isDefined)
      acc0 ::= WELCOME_AD_ID -> JsString(welcomeAdId.get)
    if (location.isDefined)
      acc0 ::= LOCATION_ESFN -> location.get.toPlayGeoJson
    JsObject(acc0)
  }

}

