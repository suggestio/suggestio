package io.suggest.ym.model.common

import io.suggest.model.common._
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

@deprecated("bycicle", "2014.04.04")
trait AdNetMemberComboStatic[T <: AdNetMemberCombo[T]]
  extends AdEntityBasicStatic[T]
  with EMNameStatic[T]
  with EMPersonIdsStatic[T]
  with EMLogoImgIdStatic[T]
  with AdNetMemberStatic[T]

@deprecated("bycicle", "2014.04.04")
trait AdNetMemberCombo[T <: AdNetMemberCombo[T]]
  extends AdEntityBasic[T]
  with EMName[T]
  with EMPersonIds[T]
  with EMLogoImgId[T]
  with AdNetMember[T]


object AdNetMember {
  val ADN_MEMBER_INFO_ESFN = "adnMemberInfo"

  // Подполя объекта
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

trait AdNetMemberStatic[T <: AdNetMember[T]] extends EsModelStaticT[T] {
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

trait AdNetMember[T <: AdNetMember[T]] extends EsModelT[T] {
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


/** Типы узлов рекламной сети. */
object AdNetMemberTypes extends Enumeration {
  type AdNetMemberType = Value

  val MART = Value("m")
  val SHOP = Value("s")
  val RESTARAUNT = Value("r")

  /** Супервизор - некий диспетчер, управляющий под-сетью. */
  val ASN_SUPERVISOR = Value("s")
}
