package io.suggest.ym.model.common

import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.ym.model.{AdNetMemberType, AdShowLevel}
import EsModel._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:40
 * Description: Базис для рекламных объектов, живущих в рекламной сети.
 */

object EMAdEntity {

  val RECEIVER_ESFN      = "receiver"
  val RECEIVER_ID_ESFN   = "receiverId"
  val PRODUCER_ID_ESFN   = "producerId"
  val PRODUCER_TYPE_ESFN = "producerType"
  val SHOW_LEVELS_ESFN   = "showLevels"

}

import EMAdEntity._


trait EMAdEntityStatic[T <: EMAdEntity[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(PRODUCER_ID_ESFN, index = FieldIndexingVariants.not_analyzed,  include_in_all = false) ::
    FieldNestedObject(RECEIVER_ESFN, enabled = true, properties = Seq(
      FieldString(RECEIVER_ID_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(SHOW_LEVELS_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false)
    )) ::
    FieldString(PRODUCER_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (PRODUCER_ID_ESFN, value) =>
        acc.producerId = stringParser(value)
      case (RECEIVER_ESFN, value) =>
        acc.receivers = AdReceiverInfo.deserializeAll(value)
      case (PRODUCER_TYPE_ESFN, value) =>
        acc.producerType = AdNetMemberTypes.withName(stringParser(value))
    }
  }

  def producerIdQuery(producerId: String) = QueryBuilders.termQuery(PRODUCER_ID_ESFN, producerId)

  /**
   * Найти все рекламные карточки, принадлежащие (созданные) указанным узлом рекламной сети.
   * @param adnId id узла, создавшего рекламные карточки.
   * @return Список результатов.
   */
  def findForProducer(adnId: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery(producerIdQuery(adnId))
      .addSort(DATE_CREATED_ESFN, SortOrder.DESC)
      .execute()
      .map { searchResp2list }
  }

}


/** Интерфейс абстрактной карточки. */
trait EMAdEntity[T <: EsModelMinimalT[T]] extends EsModelT[T] {

  /** Кто является изготовителем этой рекламной карточки? */
  var producerId: String

  /** Где (у кого) должна отображаться эта рекламная карточка? */
  var receivers: Set[AdReceiverInfo]

  /** Тип создателя рекламы. */
  var producerType: AdNetMemberType

  abstract override def writeJsonFields(acc: XContentBuilder) = {
    acc.field(PRODUCER_ID_ESFN, producerId)
    if (!receivers.isEmpty) {
      val rcvrsRaw = JacksonWrapper.serialize(receivers)
      acc.rawField(RECEIVER_ESFN, rcvrsRaw.getBytes)
    }
    acc.field(PRODUCER_TYPE_ESFN, producerType)
    super.writeJsonFields(acc)
  }
}


object AdReceiverInfo {
  val deserialize: PartialFunction[AnyRef, AdReceiverInfo] = {
    case v: java.util.Map[_,_] =>
      AdReceiverInfo(
        receiverId = v.get(RECEIVER_ID_ESFN).toString,
        showLevels = EMShowLevels.deserializeShowLevels(v.get(SHOW_LEVELS_ESFN))
      )
  }

  val deserializeAll: PartialFunction[AnyRef, Set[AdReceiverInfo]] = {
    case i: java.lang.Iterable[_] =>
      i.map(deserialize(_)).toSet
  }
}

case class AdReceiverInfo(
  var receiverId: String,
  var showLevels: Set[AdShowLevel]
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
  private def toJson: XContentBuilder = {
    val acc = XContentFactory.jsonBuilder().startObject()
      .field(RECEIVER_ID_ESFN, receiverId)
    if (!showLevels.isEmpty) {
      acc.startArray(SHOW_LEVELS_ESFN)
      showLevels.foreach { sl =>
        acc.value(sl.toString)
      }
      acc.endArray()
    }
    acc
  }
}

