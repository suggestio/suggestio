package io.suggest.ym.model.common

import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import io.suggest.model._
import io.suggest.ym.model.AdShowLevel
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:40
 * Description: Поле receivers содержит описание рекламных целей-субъектов,
 * т.е. участников рекламной сети, которые будут отображать эту рекламу.
 * Т.е. ресиверы ("приёмники") - это адресаты рекламных карточек.
 */

object EMReceivers {

  val RECEIVER_ESFN      = "receiver"
  val RECEIVER_ID_ESFN   = "receiverId"
  val SHOW_LEVELS_ESFN   = "showLevels"

}

import EMReceivers._


trait EMReceiversStatic[T <: EMReceiversMut[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    FieldNestedObject(RECEIVER_ESFN, enabled = true, properties = Seq(
      FieldString(RECEIVER_ID_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(SHOW_LEVELS_ESFN, FieldIndexingVariants.not_analyzed, include_in_all = false)
    )) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (RECEIVER_ESFN, value) =>
        acc.receivers = AdReceiverInfo.deserializeAll(value)
    }
  }

}


/** Интерфейс абстрактной карточки. */
trait EMReceivers[T <: EsModelMinimalT[T]] extends EsModelT[T] {

  /** Где (у кого) должна отображаться эта рекламная карточка? */
  def receivers: Set[AdReceiverInfo]

  abstract override def writeJsonFields(acc: XContentBuilder) = {
    super.writeJsonFields(acc)
    if (!receivers.isEmpty) {
      acc.startArray(RECEIVER_ESFN)
      receivers.foreach { rcvr =>
        rcvr.toXContent(acc)
      }
      acc.endArray()
      // Возможно, без jackson работать нормально не будет, поэтому тут исходная версия сериализации:
      //val rcvrsRaw = JacksonWrapper.serialize(receivers)
      //acc.rawField(RECEIVER_ESFN, rcvrsRaw.getBytes)
    }
  }
}

trait EMReceiversMut[T <: EMReceiversMut[T]] extends EMReceivers[T] {
  var receivers: Set[AdReceiverInfo]
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
  def toXContent(acc: XContentBuilder): XContentBuilder = {
    acc.startObject()
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

