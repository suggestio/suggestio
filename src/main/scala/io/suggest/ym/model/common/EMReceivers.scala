package io.suggest.ym.model.common

import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.ym.model.AdShowLevel
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:40
 * Description: Поле receivers содержит описание рекламных целей-субъектов,
 * т.е. участников рекламной сети, которые будут отображать эту рекламу.
 * Т.е. ресиверы ("приёмники") - это адресаты рекламных карточек.
 */

object EMReceivers {

  val RECEIVERS_ESFN   = "receivers"
  val RECEIVER_ID_ESFN = "receiverId"
  
  /** Название поля, содержащего уровни отображения, желаемые продьюсером контента. */ 
  val SLS_WANT_ESFN    = "slWant"
  
  /** Название поля, содержащего уровни отображения, выставленные системой на основе желаемых и разрешенных. */
  val SLS_PUB_ESFN     = "slPub"

  // Полные имена полей (используются при составлении поисковых запросов).
  private def fullFN(fn: String) = RECEIVERS_ESFN + "." + fn

  val RCVRS_RECEIVER_ID_ESFN  = fullFN(RECEIVER_ID_ESFN)
  val RCVRS_SLS_WANT_ESFN     = fullFN(SLS_WANT_ESFN)
  val RCVRS_SLS_PUB_ESFN      = fullFN(SLS_PUB_ESFN)

}

import EMReceivers._


trait EMReceiversStatic[T <: EMReceiversMut[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    FieldNestedObject(RECEIVERS_ESFN, enabled = true, properties = Seq(
      FieldString(RECEIVER_ID_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(SLS_WANT_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(SLS_PUB_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false)
    )) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (RECEIVERS_ESFN, value) =>
        acc.receivers = AdReceiverInfo.deserializeAll(value)
    }
  }

  def receiverIdQuery(receiverId: String) = QueryBuilders.termQuery(RCVRS_RECEIVER_ID_ESFN, receiverId)

  /**
   * Накатить сверху на запрос дополнительный фильтр для уровней.
   * @param query0 Исходный поисковый запрос.
   * @param isPub Если true, то будет поиск по published-уровням. Иначе по want-уровням.
   * @param withLevels Список допустимых уровней. Если пусто, то будет фильтр, ищущий карточки без уровней вообще.
   * @param useAnd Использовать "and"-оператор для термов. Если false, то будет "or".
   * @return QueryBuilder
   */
  def withLevelsFilter(query0: QueryBuilder, isPub: Boolean, withLevels: Seq[AdShowLevel], useAnd: Boolean): QueryBuilder = {
    // Нужен фильтр по уровням.
    val fn = if (isPub) {
      RCVRS_SLS_PUB_ESFN
    } else {
      RCVRS_SLS_WANT_ESFN
    }
    val lvlFilter = if (withLevels.isEmpty) {
      // нужна реклама без уровней вообще
      FilterBuilders.missingFilter(fn)
    } else {
      FilterBuilders.termsFilter(fn, withLevels : _*)
        .execution(if (useAnd) "and" else "or")
    }
    val nestedLvlFilter = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, lvlFilter)
    QueryBuilders.filteredQuery(query0, nestedLvlFilter)
  }

}


/** Интерфейс абстрактной карточки. */
trait EMReceivers[T <: EMReceivers[T]] extends EsModelT[T] {

  /** Где (у кого) должна отображаться эта рекламная карточка? */
  def receivers: Set[AdReceiverInfo]

  abstract override def writeJsonFields(acc: XContentBuilder) = {
    super.writeJsonFields(acc)
    if (!receivers.isEmpty) {
      writeFieldReceivers(acc)
    }
  }

  /** Рендер поля receivers. Вынесен из [[writeJsonFields]] для удобства выполнения апдейта только уровней. */
  def writeFieldReceivers(acc: XContentBuilder): XContentBuilder = {
    acc.startArray(RECEIVERS_ESFN)
      receivers.foreach { rcvr =>
        rcvr.toXContent(acc)
      }
    acc.endArray()
  }

  /** Поле slsPub обычно выставляется на основе want-поля и списка разрешенных уровней. Их пересечение
    * является новым slsPub значением. Тут короткий враппер, чтобы это всё сделать. */
  def resetReceiversSlsPub(slsAllowed: Set[AdShowLevel]) {
    receivers.foreach { rcvr =>
      rcvr.slsPub = rcvr.slsWant intersect slsAllowed
    }
  }
}

trait EMReceiversMut[T <: EMReceiversMut[T]] extends EMReceivers[T] {
  var receivers: Set[AdReceiverInfo]
}


/** Статическая поддержка AdReceiversInfo. */
object AdReceiverInfo {

  /** Десериализация сериализованного AdReceiversInfo. */
  val deserialize: PartialFunction[Any, AdReceiverInfo] = {
    case v: java.util.Map[_,_] =>
      AdReceiverInfo(
        receiverId = v.get(RECEIVER_ID_ESFN).toString,
        slsWant = EMShowLevels.deserializeShowLevels(v.get(SLS_WANT_ESFN)),
        slsPub = EMShowLevels.deserializeShowLevels(v.get(SLS_PUB_ESFN))
      )
  }

  /** Десериализация сериализованного массива/списка AdReceiversInfo. */
  val deserializeAll: PartialFunction[AnyRef, Set[AdReceiverInfo]] = {
    case i: java.lang.Iterable[_] =>
      i.map(deserialize(_)).toSet
  }

  /** Хелпер для безусловной сериализации поля с набором уровней. */
  def serializeLevels(name: String, sls: Iterable[AdShowLevel], acc: XContentBuilder): XContentBuilder = {
    acc.startArray(name)
    sls foreach { sl =>
       acc.value(sl.toString)
    }
    acc.endArray()
  }

  /** Хелпер для сериализации поля с набором уровней, если тот содержит данные. */
  def maybeSerializeLevels(name: String, sls: Iterable[AdShowLevel], acc: XContentBuilder): XContentBuilder = {
    if (sls.isEmpty)
      acc
    else
      serializeLevels(name, sls, acc)
  }
}

import AdReceiverInfo._

/**
 * Экземляр информации об одном получателе рекламы.
 * @param receiverId id получателя.
 * @param slsWant Желаемые уровни отображения.
 * @param slsPub Выставленные системой уровни отображения на основе желаемых и возможных уровней.
 */
case class AdReceiverInfo(
  var receiverId: String,
  var slsWant: Set[AdShowLevel] = Set.empty,
  var slsPub: Set[AdShowLevel]  = Set.empty
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
  def toXContent(acc: XContentBuilder): XContentBuilder = {
    acc.startObject()
      .field(RECEIVER_ID_ESFN, receiverId)
    maybeSerializeLevels(SLS_WANT_ESFN, slsWant, acc)
    maybeSerializeLevels(SLS_PUB_ESFN, slsPub, acc)
  }

}

