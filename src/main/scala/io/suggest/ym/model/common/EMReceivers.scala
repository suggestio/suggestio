package io.suggest.ym.model.common

import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import io.suggest.util.{Lists, SioConstants}
import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.ym.model.AdShowLevel
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.ym.model.common.AdnSinks.AdnSink
import io.suggest.ym.model.common.SinkShowLevels.SinkShowLevel
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.{FilterBuilder, FilterBuilders, QueryBuilder, QueryBuilders}
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

object EMReceivers extends PrefixedFn {

  type Receivers_t = Map[String, AdReceiverInfo]

  val RECEIVERS_ESFN   = "receivers"
  val RECEIVER_ID_ESFN = "receiverId"

  /** Поле, содержащее уровни отображения карточки на всех синках. */
  val SLS_ESFN         = "sls"

  override protected def _PARENT_FN = RECEIVERS_ESFN

  def RCVRS_RECEIVER_ID_ESFN  = _fullFn(RECEIVER_ID_ESFN)
  def RCVRS_SLS_ESFN          = _fullFn(SLS_ESFN)


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


trait EMReceiversStatic extends EsModelStaticMutAkvT {

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



trait EMReceiversI extends EsModelPlayJsonT {
  override type T <: EMReceiversI

  /** Где (у кого) должна отображаться эта рекламная карточка? */
  def receivers: Receivers_t

  /** Есть ли хоть один уровень в каком-либо published? */
  def isPublished: Boolean = {
    receivers.exists {
      case (_, ari)  =>  ari.sls.nonEmpty
    }
  }

  /** Просуммировать уровни отображения в контексте sink'ов. */
  def allSinkShowLevels: Set[SinkShowLevel] = {
    receivers.foldLeft (Set.empty[SinkShowLevel]) {
      case (acc, (_, ari))  =>  acc union ari.sls
    }
  }

  /** Пришло на смену методам allWantShowLevels() и allPubShowLevels(). */
  def allShowLevels: Set[AdShowLevel] = allSinkShowLevels map { _.sl }

  /** Опубликована ли рекламная карточка у указанного получателя? */
  def isPublishedAt(receiverId: String): Boolean = {
    receivers.get(receiverId)
      .exists(_.sls.nonEmpty)
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
      i.iterator()
        .map { ii =>
          val ari = deserialize(ii)
          ari.receiverId -> ari
        }
        .toMap
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

  /** Напечатать карту ресиверов в человеко-читабельный текст.
    * @param receivers Карта ресиверов.
    * @return Строка без переносов строк.
    */
  def formatReceiversMapPretty(receivers: Receivers_t): String = {
    receivers.toSeq.sortBy(_._1).mkString("Rcvrs(\n",  ",\n    ",  "\n)")
  }

  /**
   * Мержить карты ресиверов надо через этот метод, а НЕ через ++.
   * @param maps Карты ресиверов.
   * @return Единая Карта Ресиверов.
   */
  def mergeRcvrMaps(maps: Receivers_t*): Receivers_t = {
    Lists.mergeMaps(maps: _*) { (_, ri1, ri2) =>
      ri1.copy(
        sls = ri1.sls ++ ri2.sls
      )
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

  def allShowLevels = sls.map(_.sl)
  def allSinks = sls.map(_.adnSink)

  /**
   * sls отфильтровать по указанному синку.
   * @param sink Sink выдачи.
   * @return Множество SinkShowLevel, у которых adnSink == sink.
   */
  def slsOnSink(sink: AdnSink): Set[SinkShowLevel] = {
    sls.filter(_.adnSink == sink)
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

  override def toString: String = {
    val rcvrsFmt = sls.iterator.map(_.name).mkString(", ")
    s"ri[$receiverId -> $rcvrsFmt]"
  }
}



/** DynSearch-Аддон для поиска по ресиверам. */
trait ReceiversDsa extends DynSearchArgs {

  /** id "получателя" рекламы, т.е. id ТЦ, ресторана и просто поискового контекста. */
  def receiverIds: Seq[String]

  /** Какого уровня требуются карточки. */
  def levels: Seq[SlNameTokenStr]

  /** Добавить exists или missing фильтр на выходе, который будет убеждаться, что в индексе есть или нет id ресиверов. */
  def anyReceiverId: Option[Boolean]

  /** Добавить exists/missing фильтр на выходе, который будет убеждаться, что уровни присуствуют или отсутствуют. */
  def anyLevel: Option[Boolean]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map { qb =>
      // Если receiverId задан, то надо фильтровать в рамках ресивера. Сразу надо уровни отработать, т.к. они nested в одном поддокументе.
      if (receiverIds.nonEmpty || levels.nonEmpty) {
        var nestedSubfilters: List[FilterBuilder] = Nil
        if (receiverIds.nonEmpty) {
          nestedSubfilters ::= FilterBuilders.termsFilter(EMReceivers.RCVRS_RECEIVER_ID_ESFN, receiverIds: _*)
        }
        if (levels.nonEmpty) {
          nestedSubfilters ::= FilterBuilders.termsFilter(EMReceivers.RCVRS_SLS_ESFN, levels.map(_.name) : _*)
        }
        // Если получилось несколько фильтров, то надо их объеденить.
        val finalNestedSubfilter: FilterBuilder = if (nestedSubfilters.tail.nonEmpty) {
          FilterBuilders.andFilter(nestedSubfilters: _*)
        } else {
          nestedSubfilters.head
        }
        // Оборачиваем результирующий фильтр в nested, и затем вешаем на исходную query.
        val nestedFilter = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, finalNestedSubfilter)
        QueryBuilders.filteredQuery(qb, nestedFilter)
      } else {
        qb
      }

    }.orElse[QueryBuilder] {
      // Нет поискового запроса. Попытаться собрать запрос по ресиверу с опциональным фильтром по level.
      if (receiverIds.nonEmpty) {
        var nestedSubquery: QueryBuilder = QueryBuilders.termsQuery(EMReceivers.RCVRS_RECEIVER_ID_ESFN, receiverIds : _*)
        if (levels.nonEmpty) {
          val levelFilter = FilterBuilders.termsFilter(EMReceivers.RCVRS_SLS_ESFN, levels.map(_.name) : _*)
          nestedSubquery = QueryBuilders.filteredQuery(nestedSubquery, levelFilter)
        }
        val qb = QueryBuilders.nestedQuery(EMReceivers.RECEIVERS_ESFN, nestedSubquery)
        Some(qb)

      } else if (levels.nonEmpty) {
        val levelQuery = QueryBuilders.termsQuery(EMReceivers.SLS_ESFN, levels.map(_.name) : _*)
        val qb = QueryBuilders.nestedQuery(EMReceivers.RECEIVERS_ESFN, levelQuery)
        Some(qb)

      } else {
        None
      }
    }
  }

  override def toEsQuery: QueryBuilder = {
    var query = super.toEsQuery
    // Если задан anyReceiverId, то нужно добавить exists/missing-фильтр для проверки состояния значений в rcvrs.id.
    if (anyReceiverId.isDefined) {
      val fn = EMReceivers.RCVRS_RECEIVER_ID_ESFN
      val f = if (anyReceiverId.get) {
        FilterBuilders.existsFilter(fn)
      } else {
        FilterBuilders.missingFilter(fn)
      }
      val nf = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, f)
      query = QueryBuilders.filteredQuery(query, nf)
    }
    // Если задан anyLevel, то нужно добавиль фильтр по аналогии с anyReceiverId.
    if (anyLevel.isDefined) {
      val fn = EMReceivers.RCVRS_SLS_ESFN
      val f = if(anyLevel.get) {
        FilterBuilders.existsFilter(fn)
      } else {
        FilterBuilders.missingFilter(fn)
      }
      val nf = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, f)
      query = QueryBuilders.filteredQuery(query, nf)
    }
    query
  }

}
trait ReceiversDsaDflt extends ReceiversDsa {
  override def receiverIds    : Seq[String]           = Nil
  override def levels         : Seq[SlNameTokenStr]   = Nil
  override def anyReceiverId  : Option[Boolean]       = None
  override def anyLevel       : Option[Boolean]       = None
}
trait ReceiversDsaWrapper extends ReceiversDsa with DynSearchArgsWrapper {
  override type WT <: ReceiversDsa
  override def receiverIds    = _dsArgsUnderlying.receiverIds
  override def levels         = _dsArgsUnderlying.levels
  override def anyReceiverId  = _dsArgsUnderlying.anyReceiverId
  override def anyLevel       = _dsArgsUnderlying.anyLevel
}

/** Генератор самого дефолтового запроса, когда toEsQueryOpt не смог ничего предложить.
  * Нужно отображать только карточки, которые опубликованы где-либо. */
trait ReceiversDsaOnlyPublishedByDefault extends DynSearchArgs {
  override def defaultEsQuery: QueryBuilder = {
    val q0 = super.defaultEsQuery
    val f = FilterBuilders.existsFilter(EMReceivers.RCVRS_SLS_ESFN)
    val nf = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, f)
    QueryBuilders.filteredQuery(q0, nf)
  }
}