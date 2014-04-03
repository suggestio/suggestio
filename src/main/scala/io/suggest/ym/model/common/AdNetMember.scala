package io.suggest.ym.model.common

import io.suggest.model.common._
import io.suggest.ym.model.common.AdNetMemberTypes.AdNetMemberType

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:03
 * Description: Объект-участнник рекламной сети с произвольной ролью. Это может быть ТЦ, магазин или кто-то ещё.
 * Таким объектом владеют люди, и совершают действия от имени объекта.
 */

trait AdNetMemberStatic[T <: AdNetMember[T]]
  extends AdEntityBasicStatic[T]
  with EMNameStatic[T]
  with EMPersonIdsStatic[T]
  with EMLogoImgIdStatic[T]

trait AdNetMember[T <: AdNetMember[T]]
  extends AdEntityBasic[T]
  with EMName[T]
  with EMPersonIds[T]
  with EMLogoImgId[T]
{

  def isAdProducer: Boolean
  def isAdReceiver: Boolean
  def aNMType: AdNetMemberType
  def isAdNSupervisor: Boolean
  def aNMSupId: Option[String]
}


/** Типы узлов рекламной сети. */
object AdNetMemberTypes extends Enumeration {
  type AdNetMemberType = Value

  val MART = Value("m")
  val SHOP = Value("s")
  val RESTARAUNT = Value("r")

  /** Супервизор - некий диспетчер, управляющий под-сетью. */
  val ASN_SUPERVISOR = Value("s")
}
