package io.suggest.xadv.ext.js.vk.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.sjs.common.model.{FromJsonT, IToJsonDict}

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 14:46
 * Description: Модель, представляющая рантаймовую инфу по цели размещения.
 * Для вконтакта это целочисленный id и тип.
 */
object VkTargetInfo extends FromJsonT {

  def ID_FN       = "z"
  def TG_TYPE_FN  = "y"
  def TG_NAME_FN  = "x"

  override type T = VkTargetInfo

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]]
    VkTargetInfo(
      id      = d(ID_FN).asInstanceOf[Int],
      tgType  = VkTargetTypes.withValue( d(TG_TYPE_FN).toString ),
      name    = d.get(TG_NAME_FN).map(_.toString)
    )
  }
}

import VkTargetInfo._

/**
 * Рантаймовая инфа по цели размещения.
 * @param id внутренний vk id цели.
 * @param tgType Тип цели (юзер, группа, ...).
 */
case class VkTargetInfo(id: UserId_t, tgType: VkTargetType, name: Option[String]) extends IToJsonDict {
  override def toJson = {
    val d = Dictionary[Any](
      ID_FN         -> id,
      TG_TYPE_FN    -> tgType.vkName
    )
    if (name.isDefined)
      d.update(TG_NAME_FN, name.get)
    d
  }
}


/**
 * Типы объектов по мнению вконтакта. Типы резолвятся через VK API utils.resolveScreenName.
 * @see [[https://vk.com/dev/utils.resolveScreenName]]
 */
object VkTargetTypes extends StringEnum[VkTargetType] {

  /** Страница обычного юзера. */
  case object User extends VkTargetType("user") {
    override def isUser = true
  }

  /** Страница группы. */
  case object Group extends VkTargetType("group")

  /** Страница приложения. */
  case object App extends VkTargetType("application") {
    override def isGroup = false
  }

  /** Просто страница, отображается в группах. */
  case object Page extends VkTargetType("page")

  /** Некие события. */
  case object Event extends VkTargetType("event")


  override def values = findValues

}

sealed abstract class VkTargetType(override val value: String) extends StringEnumEntry {
  /** Название в терминах вконтакта. */
  @inline final def vkName: String = value
  def isUser: Boolean = false
  def isGroup: Boolean = !isUser
  override final def toString = vkName
}
