package io.suggest.ym.model.common

import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.ym.model.{MAdnNode, AdShowLevel}
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}
import io.suggest.ym.ad.ShowLevelsUtil
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.client.Client
import org.elasticsearch.action.update.UpdateRequestBuilder
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._

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


  /**
   * Реалтаймовый поиск по получателю.
   * @param receiverId id получателя.
   * @param maxResults Макс. кол-во результатов.
   * @return Последовательность MAd.
   */
  def findForReceiverRt(receiverId: String, maxResults: Int = 100)(implicit ec: ExecutionContext, client: Client): Future[List[T]] = {
    findQueryRt(receiverIdQuery(receiverId), maxResults)
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
  def receivers: Receivers_t


  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (!receivers.isEmpty)
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

  /** Поле slsPub обычно выставляется на основе want-поля и списка разрешенных уровней. Их пересечение
    * является новым slsPub значением. Тут короткий враппер, чтобы это всё сделать. */
  def resetReceiversSlsPub(slsAllowed: Set[AdShowLevel]) {
    receivers.valuesIterator.foreach { rcvr =>
      rcvr.slsPub = rcvr.slsWant intersect slsAllowed
    }
  }

  /** Генерация update-реквеста на обновление только поля receivers. Сам реквест не вызывается. */
  def updateReceiversReqBuilder(implicit client: Client): UpdateRequestBuilder = {
    val json = JsObject(Seq(
      RECEIVERS_ESFN -> writeReceiversPlayJson
    ))
    prepareUpdate.setDoc(json.toString())
  }

  def updateAllWantLevels(f: Set[AdShowLevel] => Set[AdShowLevel]) {
    receivers.valuesIterator.foreach { ari =>
      ari.slsWant = f(ari.slsWant)
    }
  }

  /** Сохранить новые ресиверы через update. */
  def saveReceivers(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    updateReceiversReqBuilder.execute()
  }

  /** Есть ли хоть один уровень в каком-либо published? */
  def isPublished: Boolean = receivers.exists {
    case (_, ari)  =>  !ari.slsPub.isEmpty
  }

  /** Собрать все уровни отображения со всех ресиверов. */
  def allPubShowLevels = receivers.foldLeft[Set[AdShowLevel]] (Set.empty) {
    case (acc, (_, ari))  =>  acc union ari.slsPub
  }

  /** Собрать все уровни отображения со всех ресиверов. */
  def allWantShowLevels = receivers.foldLeft[Set[AdShowLevel]] (Set.empty) {
    case (acc, (_, ari))  =>  acc union ari.slsWant
  }

  /** Опубликована ли рекламная карточка у указанного получателя? */
  def isPublishedAt(receiverId: String): Boolean = {
    receivers.get(receiverId).exists(!_.slsPub.isEmpty)
  }
}

trait EMReceiversMut[T <: EMReceiversMut[T]] extends EMReceivers[T] {
  var receivers: Receivers_t
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
 * @param slsWant Желаемые уровни отображения.
 * @param slsPub Выставленные системой уровни отображения на основе желаемых и возможных уровней.
 */
case class AdReceiverInfo(
  receiverId: String,
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
  def toPlayJson: JsObject = {
    var acc: FieldsJsonAcc = List(RECEIVER_ID_ESFN -> JsString(receiverId))
    acc = maybeSerializeLevelsPlayJson(SLS_WANT_ESFN, slsWant, acc)
    acc = maybeSerializeLevelsPlayJson(SLS_PUB_ESFN, slsPub, acc)
    JsObject(acc)
  }

}

