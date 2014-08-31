package io.suggest.ym.model.common

import io.suggest.util.SioConstants
import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.ym.model.AdShowLevel
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.ym.model.common.SinkShowLevels.SinkShowLevel
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.client.Client
import org.elasticsearch.action.update.UpdateRequestBuilder
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._
import org.elasticsearch.search.aggregations.{Aggregations, AggregationBuilders}
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:40
 * Description: Поле receivers содержит описание рекламных целей-субъектов,
 * т.е. участников рекламной сети, которые будут отображать эту рекламу.
 * Т.е. ресиверы ("приёмники") - это адресаты рекламных карточек.
 */

object EMReceivers {

  type Receivers_t = Map[String, AdReceiverInfo]

  val RECEIVERS_ESFN   = "receivers"
  val RECEIVER_ID_ESFN = "receiverId"

  /** Поле, содержащее уровни отображения карточки на всех синках. */
  val SLS_ESFN         = "sls"


  // Полные имена полей (используются при составлении поисковых запросов).
  private def fullFN(fn: String) = RECEIVERS_ESFN + "." + fn

  def RCVRS_RECEIVER_ID_ESFN  = fullFN(RECEIVER_ID_ESFN)
  def RCVRS_SLS_ESFN          = fullFN(SLS_ESFN)


  /** Собрать аггрегатор для сбора receivers.receiverId. */
  def receiverIdsAgg = {
    val subagg = AggregationBuilders.terms(RECEIVER_ID_ESFN)
      .field(RCVRS_RECEIVER_ID_ESFN)
    AggregationBuilders.nested(RECEIVERS_ESFN)
      .path(RECEIVERS_ESFN)
      .subAggregation(subagg)
  }

  /**
   * Превратить в карту выхлоп аггрегации поиского запроса, созданного на базе receiverIdsAgg().
   * @param aggs Выхлоп аггрегации ES.
   * @return Карта (receiverId: String -> docCount: Long).
   */
  def extractReceiverIdsAgg(aggs: Aggregations): Map[String, Long] = {
    aggs.get[Nested](RECEIVERS_ESFN)
      .getAggregations
      .get[Terms](RECEIVER_ID_ESFN)
      .getBuckets
      .iterator()
      .foldLeft [List[(String, Long)]] (Nil) {
        (acc, bkt)  =>  bkt.getKey -> bkt.getDocCount :: acc
      }
      .toMap
  }

  def receiverIdQuery(receiverId: String) = {
    val q = QueryBuilders.termQuery(RCVRS_RECEIVER_ID_ESFN, receiverId)
    QueryBuilders.nestedQuery(RECEIVERS_ESFN, q)
  }
}

import EMReceivers._


trait EMReceiversStatic extends EsModelStaticT {

  override type T <: EMReceiversMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldNestedObject(RECEIVERS_ESFN, enabled = true, properties = Seq(
      FieldString(RECEIVER_ID_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(SLS_ESFN,  index = FieldIndexingVariants.analyzed,  include_in_all = false,
        index_analyzer = SioConstants.DEEP_NGRAM_AN,  search_analyzer = SioConstants.MINIMAL_AN)
    )) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (RECEIVERS_ESFN, value) =>
        acc.receivers = AdReceiverInfo.deserializeAll(value)
    }
  }


  /** Сохранить все значения ресиверов со всех переданных карточек в хранилище модели.
    * Другие поля не будут обновляться. Для ускорения и некоторого подобия транзакционности делаем всё через bulk. */
  def updateAllReceivers(mads: Seq[T])(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val bulkRequest = client.prepareBulk()
    mads.foreach { mad =>
      val updReq = mad.updateReceiversReqBuilder
      bulkRequest.add(updReq)
    }
    bulkRequest.execute()
  }

}


trait EMReceiversI extends EsModelT {
  override type T <: EMReceiversI

  /** Где (у кого) должна отображаться эта рекламная карточка? */
  def receivers: Receivers_t

  /** Есть ли хоть один уровень в каком-либо published? */
  def isPublished: Boolean = receivers.exists {
    case (_, ari)  =>  ari.sls.nonEmpty
  }

  /** Просуммировать уровни отображения в контексте sink'ов. */
  def allSinkShowLevels = receivers.foldLeft [Set[SinkShowLevel]] (Set.empty) {
    case (acc, (_, ari))  =>  acc union ari.sls
  }

  /** Пришло на смену методам allWantShowLevels() и allPubShowLevels(). */
  def allShowLevels: Set[AdShowLevel] = allSinkShowLevels map { _.sl }

  /** Опубликована ли рекламная карточка у указанного получателя? */
  def isPublishedAt(receiverId: String): Boolean = {
    receivers.get(receiverId).exists(_.sls.nonEmpty)
  }
}


/** Интерфейс абстрактной карточки. */
trait EMReceivers extends EMReceiversI {

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (receivers.nonEmpty)
      RECEIVERS_ESFN -> writeReceiversPlayJson :: acc0
    else
      acc0
  }

  def writeReceiversPlayJson: JsArray = {
    val arrayElements = receivers.valuesIterator
      .map(_.toPlayJson)
      .toSeq
    JsArray(arrayElements)
  }

  /** Генерация update-реквеста на обновление только поля receivers. Сам реквест не вызывается. */
  def updateReceiversReqBuilder(implicit client: Client): UpdateRequestBuilder = {
    val json = JsObject(Seq(
      RECEIVERS_ESFN -> writeReceiversPlayJson
    ))
    prepareUpdate.setDoc(json.toString())
  }

}

trait EMReceiversMut extends EMReceivers {
  override type T <: EMReceiversMut
  var receivers: Receivers_t
}


/** Статическая поддержка AdReceiversInfo. */
object AdReceiverInfo {

  /** Десериализация сериализованного AdReceiversInfo. */
  val deserialize: PartialFunction[Any, AdReceiverInfo] = {
    case v: java.util.Map[_,_] =>
      AdReceiverInfo(
        receiverId = v.get(RECEIVER_ID_ESFN).toString,
        sls = Option(v get SLS_ESFN)
          .map ( SinkShowLevels.deserializeLevelsSet )
          .orElse { Option(v get SLS_ESFN).map { SinkShowLevels.deserializeFromAdSls } }
          .getOrElse { Set.empty }
      )
  }

  /** Десериализация сериализованного массива/списка AdReceiversInfo. */
  val deserializeAll: PartialFunction[AnyRef, Receivers_t] = {
    case i: java.lang.Iterable[_] =>
      i.map { ii =>
        val ari = deserialize(ii)
        ari.receiverId -> ari
      }.toMap
  }

  def maybeSerializeLevelsPlayJson(name: String, sls: Iterable[AdShowLevel], acc: FieldsJsonAcc): FieldsJsonAcc = {
    if (sls.isEmpty) {
      acc
    } else {
      // Используем fold вместо toSeq + map для ускорения работы.
      val arrayElements = sls.foldLeft[List[JsString]] (Nil) {
        (acc, e)  =>  JsString(e.toString) :: acc
      }
      name -> JsArray(arrayElements) :: acc
    }
  }
}

import AdReceiverInfo._

/**
 * Экземляр информации об одном получателе рекламы.
 * @param receiverId id получателя.
 * @param sls sink-уровни отображения. Описывают где что отображать.
 *            Пришли на смену изрядно устаревшим велосипедам slsWant+slsPub.
 */
case class AdReceiverInfo(
  receiverId: String,
  sls: Set[SinkShowLevel] = Set.empty
) {
  override def equals(obj: scala.Any): Boolean = super.equals(obj) || {
    obj match {
      case ari: AdReceiverInfo => ari.receiverId == receiverId
      case _ => false
    }
  }

  @JsonIgnore
  override def hashCode(): Int = receiverId.hashCode

  @JsonIgnore
  def toPlayJson: JsObject = {
    var acc: FieldsJsonAcc = List(RECEIVER_ID_ESFN -> JsString(receiverId))
    if (sls.nonEmpty) {
      val slsStr = sls.foldLeft(List.empty[JsString]) {
        (acc, sl)  =>  JsString(sl.name) :: acc
      }
      acc ::= SLS_ESFN -> JsArray(slsStr)
    }
    JsObject(acc)
  }

}

