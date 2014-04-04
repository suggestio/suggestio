package io.suggest.ym.model.common

import io.suggest.ym.model.common.AdNetMemberTypes.AdNetMemberType
import io.suggest.model.{EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:03
 * Description: Объект-участнник рекламной сети с произвольной ролью. Это может быть ТЦ, магазин или кто-то ещё.
 * Таким объектом владеют люди, и совершают действия от имени объекта.
 */

object AdNetMember {
  
  /** Название root-object поля, в котором хранятся данные по участию в рекламной сети. */
  val ADN_MEMBER_INFO_ESFN = "adnMemberInfo"

  // Имена полей вышеуказнного объекта
  val IS_PRODUCER_ESFN    = "isProd"
  val IS_RECEIVER_ESFN    = "isRcvr"
  val IS_SUPERVISOR_ESFN  = "isSup"
  val SUPERVISOR_ID_ESFN  = "supId"
  val MEMBER_TYPE_ESFN    = "mType"

  private def fullFN(subFN: String): String = ADN_MEMBER_INFO_ESFN + "." + subFN

  // Абсолютные (плоские) имена полей. Используются при поиске.
  val ADN_MI_IS_PRODUCER_ESFN   = fullFN(IS_PRODUCER_ESFN)
  val ADN_MI_IS_RECEIVER_ESFN   = fullFN(IS_RECEIVER_ESFN)
  val ADN_MI_IS_SUPERVISOR_ESFN = fullFN(IS_SUPERVISOR_ESFN)
  val ADN_MI_SUPERVISOR_ID_ESFN = fullFN(SUPERVISOR_ID_ESFN)
  val ADN_MI_MEMBER_TYPE_ESFN   = fullFN(MEMBER_TYPE_ESFN)

}

import AdNetMember._

/** Трейт для статической части модели участника рекламной сети. */
trait EMAdNetMemberStatic[T <: EMAdNetMember[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    import FieldIndexingVariants.not_analyzed
    FieldObject(ADN_MEMBER_INFO_ESFN, enabled = true, properties = Seq(
      FieldBoolean(IS_PRODUCER_ESFN, index = not_analyzed, include_in_all = false),
      FieldBoolean(IS_RECEIVER_ESFN, index = not_analyzed, include_in_all = false),
      FieldBoolean(IS_SUPERVISOR_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(SUPERVISOR_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(MEMBER_TYPE_ESFN, index = not_analyzed, include_in_all = false)
    )) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = super.applyKeyValue(acc) orElse {
    case (ADN_MEMBER_INFO_ESFN, value) =>
      acc.adnMemberInfo = JacksonWrapper.convert[AdNetMemberInfo](value)
  }
}

/** Трейт для экземпляра модели участника рекламной сети. */
trait EMAdNetMember[T <: EMAdNetMember[T]] extends EsModelT[T] {
  var adnMemberInfo: AdNetMemberInfo

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    val adnmiJson = JacksonWrapper.serialize(adnMemberInfo)
    acc.rawField(ADN_MEMBER_INFO_ESFN, adnmiJson.getBytes)
  }
}


/** Инфа об участнике рекламной сети. Все параметры его участия свернуты в один объект. */
case class AdNetMemberInfo(
  var isAdProducer: Boolean,
  var isAdReceiver: Boolean,
  var adNMType: AdNetMemberType,
  var isAdNSupervisor: Boolean,
  var adNMSupId: Option[String]
)

