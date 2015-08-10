package io.suggest.xadv.ext.js.vk.m

import io.suggest.model.LightEnumeration
import io.suggest.sjs.common.model.{IToJsonDict, FromJsonT}

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
      tgType  = VkTargetTypes.withName( d(TG_TYPE_FN).toString ),
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
object VkTargetTypes extends LightEnumeration {
  sealed protected trait ValT extends super.ValT {
    def vkName: String
    def isUser: Boolean = false
    def isGroup: Boolean = !isUser
  }

  /**
   * Экземпляр модели.
   * @param vkName Название в терминах вконтакта.
   */
  protected sealed class Val(val vkName: String) extends ValT {
    override def toString = vkName
  }


  override type T = Val

  /** Страница обычного юзера. */
  val User    = new Val("user") {
    override def isUser = true
  }

  /** Страница группы. */
  val Group   = new Val("group")

  /** Страница приложения. */
  val App     = new Val("application") {
    override def isGroup = false
  }

  /** Просто страница, отображается в группах. */
  val Page    = new Val("page")

  /** Некие события. */
  val Event   = new Val("event")

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Group.vkName  => Some(Group)
      case User.vkName   => Some(User)
      case App.vkName    => Some(App)
      case Page.vkName   => Some(Page)
      case Event.vkName  => Some(Event)
      case _             => None
    }
  }
}