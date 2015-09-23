package io.suggest.ym.model.common

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.n2.node.meta.MNodeMeta
import io.suggest.ym.model.MWelcomeAd
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import io.suggest.model.{EsModel, EsModelPlayJsonT, EsModelStaticMutAkvT}
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import scala.collection.JavaConversions._
import scala.concurrent.{Future, ExecutionContext}
import io.suggest.model.n2.node.meta.EMNodeMeta._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 13:43
 * Description: Метаданные узлов-участников рекламной сети: названия, адреса, даты и т.д.
 */

object EMAdnMMetadataStatic {

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
      import EsModel.stringParser
      MNodeMeta(
        nameOpt      = Option(jmap get NAME_ESFN) map stringParser,
        nameShortOpt = Option(jmap get NAME_SHORT_ESFN)
          .orElse(Option(jmap get "ns"))    // 2014.oct.01: Переименовано поле: ns -> sn из-за изменения в маппинге.
          .map(stringParser),
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
        color       = Option(jmap get BG_COLOR_ESFN) map stringParser,
        fgColor     = Option(jmap get FG_COLOR_ESFN) map stringParser,
        welcomeAdId = Option(jmap get WELCOME_AD_ID) map stringParser
      )
  }


}

import EMAdnMMetadataStatic._


trait EMAdnMMetadataStatic extends EsModelStaticMutAkvT {

  override type T <: EMAdnMMetadata

  abstract override def generateMappingProps: List[DocField] = {
    val f = FieldObject(META_ESFN, enabled = true, properties = MNodeMeta.generateMappingProps)
    f :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (META_ESFN, value) =>
        acc.meta = EMAdnMMetadataStatic.deserializeMNodeMeta(value)
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
    val f = META_ESFN -> Json.toJson(meta)
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


