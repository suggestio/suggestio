package io.suggest.ym.model.common

import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.ym.model.{AdShowLevel, AdProducerType}
import EsModel._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:40
 * Description: Базис для рекламных карточек.
 */


object Ad {
  val RECEIVER_ESFN      = "receiver"
  val RECEIVER_ID_ESFN   = "receiver"
  val PRODUCER_ID_ESFN   = "producerId"
  val PRODUCER_TYPE_ESFN = "producerType"
  val SHOW_LEVELS_ESFN   = "showLevels"
}


import Ad._


/** Интерфейс абстрактной карточки. */
trait Ad[T <: EsModelMinimalT[T]] extends AdEntityBasic[T] {

  /** Кто является изготовителем этой рекламной карточки? */
  def producerId: String
  def producerId_=(producerId: String)

  /** Где (у кого) должна отображаться эта рекламная карточка? */
  def receivers: Set[AdReceiverInfo]
  def receivers_=(receivers: Set[AdReceiverInfo])

  /** Тип создателя рекламы. */
  def producerType: AdProducerType
  def producerType_=(producerType: AdProducerType)

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


trait AdStatic[T <: Ad] extends AdEntityBasicStatic[T] {

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(PRODUCER_ID_ESFN, index = FieldIndexingVariants.not_analyzed,  include_in_all = false) ::
    FieldNestedObject(RECEIVER_ESFN, enabled = true, properties = Seq(
      FieldString(RECEIVER_ID_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(SHOW_LEVELS_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false)
    )) ::
    FieldString(PRODUCER_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false) ::
    super.generateMappingProps
  }

  def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = true),
    FieldSource(enabled = true)
  )

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    case (PRODUCER_ID_ESFN, value) =>
      acc.producerId = stringParser(value)
    case (RECEIVER_ESFN, value) =>
      acc.receivers = JacksonWrapper.convert[Set[AdReceiverInfo]](value)
    case (PRODUCER_TYPE_ESFN, value) =>
      acc.producerType = AdProducerTypes.withName(stringParser(value))
  }

}


/** Типы создателей рекламных карточек перечисляются здесь. */
object AdProducerTypes extends Enumeration {
  type AdProducerType = Value
  val Shop = Value("s")
  val Mart = Value("m")
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

