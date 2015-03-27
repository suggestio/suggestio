package io.suggest.adv.ext.model

import io.suggest.model.{LightEnumeration, ILightEnumeration, EnumMaybeWithName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.15 12:24
 * Description: Общий код клиентских и серверных моделей сервисов внешнего размещения.
 */
object MServices {

  def NAME_FN       = "name"
  def APP_ID_FN     = "appId"

  def VKONTAKTE_ID  = "vk"
  def FACEBOOK_ID   = "fb"
  def TWITTER_ID    = "tw"

}


trait MServicesBaseT extends ILightEnumeration {
  protected trait ValT extends super.ValT {
    val strId: String
    override def toString = strId
  }

  override type T <: ValT

  // Конкретные элементы будущего enum'a.
  /** Экземпляр сервиса вконтакта. */
  val VKONTAKTE: T

  /** Экземпляр сервиса facebook. */
  val FACEBOOK: T

  /** Экземпляр сервиса twitter. */
  val TWITTER: T
}


/** Абстрактная реализация [[MServicesBaseT]] на базе scala.Enumeration. */
trait MServicesT extends Enumeration with EnumMaybeWithName with MServicesBaseT {
  protected class Val(val strId: String) extends super.Val(strId) with ValT
  override type T <: Val
}


import MServices._


/** Легковесная реализации модели сервисов без использования scala-коллекций. */
trait MServicesLightT extends MServicesBaseT with LightEnumeration {
  override def maybeWithName(n: String): Option[T] = {
    n match {
      case FACEBOOK.strId   => Some(FACEBOOK)
      case VKONTAKTE.strId  => Some(VKONTAKTE)
      case TWITTER.strId    => Some(TWITTER)
      case _                => None
    }
  }
}
