package io.suggest.ym.model.common

import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.ym.model.{AdNetMemberType, AdShowLevel}
import EsModel._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:40
 * Description: Базис для рекламных карточек.
 */


object EMAdEntity {
  val RECEIVER_ESFN      = "receiver"
  val RECEIVER_ID_ESFN   = "receiver"
  val PRODUCER_ID_ESFN   = "producerId"
  val PRODUCER_TYPE_ESFN = "producerType"
  val SHOW_LEVELS_ESFN   = "showLevels"
}


import EMAdEntity._


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


trait EmAdEntityStatic[T <: EMAdEntity[T]] extends EsModelStaticT[T] {

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
    case (PRODUCER_ID_ESFN, value) =>
      acc.producerId = stringParser(value)
    case (RECEIVER_ESFN, value) =>
      acc.receivers = JacksonWrapper.convert[Set[AdReceiverInfo]](value)
    case (PRODUCER_TYPE_ESFN, value) =>
      acc.producerType = AdNetMemberTypes.withName(stringParser(value))
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

  override def hashCode(): Int = receiverId.hashCode
}

