package io.suggest.ym.model.common

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.ym.model.MWelcomeAd
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.model.{PrefixedFn, EsModel, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
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

object EMAdnMMetadataStatic extends PrefixedFn {

  val META_FN = "meta"

  /** Название родительского поля. */
  override protected def _PARENT_FN = META_FN

  def META_WELCOME_AD_ID_FN     = _fullFn( MNodeMeta.WELCOME_AD_ID_FN )
  def META_NAME_SHORT_NOTOK_FN  = _fullFn( MNodeMeta.NAME_SHORT_NOTOK_FN )

  /**
   * Собрать указанные значения строковых полей в аккамулятор-множество.
   * @param searchResp Экземпляр searchResponse.
   * @param fn Название поля, значение которого собираем в акк.
   * @param acc0 Начальный акк.
   * @param keepAliveMs keepAlive для курсоров на стороне сервера ES в миллисекундах.
   * @return Фьючерс с результирующим аккамулятором-множеством.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html#scrolling]]
   */
  def searchScrollResp2strSet(searchResp: SearchResponse, fn: String, firstReq: Boolean, acc0: Set[String] = Set.empty, keepAliveMs: Long = EsModel.SCROLL_KEEPALIVE_MS_DFLT)
                             (implicit ec: ExecutionContext, client: Client): Future[Set[String]] = {
    // TODO Часть кода этого метода была вынесена в EsModel.foldSearchScroll(), но не тестирована после этого.
    EsModel.foldSearchScroll(searchResp, acc0, keepAliveMs = keepAliveMs, firstReq = firstReq) {
      (acc0, hits) =>
        val acc1 = hits.getHits.foldLeft[List[String]] (Nil) { (acc, hit) =>
        hit.field(fn) match {
          case null =>
            acc
          case values =>
            values.getValues.foldLeft (acc) {
              (acc1, v)  =>  v.toString :: acc1
            }
        }
      }
      val acc2 = acc0 ++ acc1
      Future successful acc2
    }
  }
  
  import MNodeMeta._
  
  /** Десериализация сериализованного экземпляра класса AdnMMetadata. */
  val deserializeMNodeMeta: PartialFunction[Any, MNodeMeta] = {
    case jmap: ju.Map[_,_] =>
      import EsModel.{stringParser, iteratorParser}
      MNodeMeta(
        nameOpt      = Option(jmap get NAME_FN) map stringParser,
        nameShortOpt = Option(jmap get NAME_SHORT_FN)
          .orElse(Option(jmap get "ns"))    // 2014.oct.01: Переименовано поле: ns -> sn из-за изменения в маппинге.
          .map(stringParser),
        hiddenDescr = Option(jmap get HIDDEN_DESCR_FN) map stringParser,
        dateCreated = EsModel.dateTimeParser(jmap get DATE_CREATED_FN),
        town        = Option(jmap get TOWN_FN) map stringParser,
        address     = Option(jmap get ADDRESS_FN) map stringParser,
        phone       = Option(jmap get PHONE_FN) map stringParser,
        floor       = Option(jmap get FLOOR_FN) map stringParser,
        section     = Option(jmap get SECTION_FN) map stringParser,
        siteUrl     = Option(jmap get SITE_URL_FN) map stringParser,
        audienceDescr = Option(jmap get AUDIENCE_DESCR_FN) map stringParser,
        humanTrafficAvg = Option(jmap get HUMAN_TRAFFIC_AVG_FN) map EsModel.intParser,
        info        = Option(jmap get INFO_FN) map stringParser,
        color       = Option(jmap get BG_COLOR_FN) map stringParser,
        fgColor     = Option(jmap get FG_COLOR_FN) map stringParser,
        welcomeAdId = Option(jmap get WELCOME_AD_ID_FN) map stringParser,
        langs       = Option(jmap get LANGS_FN)
          .iterator
          .flatMap(iteratorParser)
          .map(stringParser)
          .toList
      )
  }


}

import EMAdnMMetadataStatic._


trait EMAdnMMetadataStatic extends EsModelStaticMutAkvT {

  override type T <: EMAdnMMetadata

  abstract override def generateMappingProps: List[DocField] = {
    val f = FieldObject(META_FN, enabled = true, properties = MNodeMeta.generateMappingProps)
    f :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (META_FN, value) =>
        acc.meta = EMAdnMMetadataStatic.deserializeMNodeMeta(value)
    }
  }

  /** Собрать все id рекламных карточек, которые встречаются во всех документах модели.
    * Внутри используется match_all query + scroll.
    * @return Множество всех значений welcomeAdId.
    */
  def findAllWelcomeAdIds(maxResultsPerStep: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[Set[String]] = {
    val fn = META_WELCOME_AD_ID_FN
    prepareScroll()
      .setQuery( QueryBuilders.matchAllQuery() )
      .setSize(maxResultsPerStep)
      .setFetchSource(false)
      .addField(fn)
      .execute()
      .flatMap { searchResp =>
        searchScrollResp2strSet(searchResp, fn, firstReq = true)
      }
  }

}


trait EMAdnMMetadata extends EsModelPlayJsonT {
  override type T <: EMAdnMMetadata

  var meta: MNodeMeta

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val f = META_FN -> Json.toJson(meta)
    f :: super.writeJsonFields(acc)
  }

  override def doEraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val fut = super.doEraseResources
    // Раньше вызывался MWelcomeAd.deleteByProducerId1by1(producerId) через событие SioNotifier.
    meta.welcomeAdId.fold(fut) { welcomeAdId =>
      MWelcomeAd.getById(welcomeAdId)
        .flatMap {
          case Some(mwa) =>
            mwa.doEraseResources
              .flatMap { _ => mwa.delete }
          case None => Future successful false
        } flatMap {
          _ => fut
        }
    }
  }
}


