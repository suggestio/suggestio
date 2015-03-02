package io.suggest.xadv.ext.js.vk.m

import io.suggest.xadv.ext.js.runner.m.IToJsonDict
import io.suggest.xadv.ext.js.vk.c.low.JSON

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 11:31
 * Description: Кастомный контекст вконтакта. Сохраняется в поле MJsCtx.custom.
 */
object VkCtx {

  def LOGIN_FN      = "a"
  def TG_INFO_FN    = "b"

  /** Десериализовать контекст. */
  def fromDyn(raw: js.Any): VkCtx = {
    val d = raw.asInstanceOf[JSON] : WrappedDictionary[js.Any]
    VkCtx(
      login = d.get(LOGIN_FN)
        .map(VkLoginResult.fromJson),
      tgInfo = d.get(TG_INFO_FN)
        .map(VkTargetInfo.fromJson)
    )
  }


  /** Десериализовать опциональный контекст. */
  def maybeFromDyn(rawOpt: Option[js.Any]): Option[VkCtx] = {
    // Можно добавить сюда обработку ошибок.
    rawOpt map fromDyn
  }

}

import VkCtx._


/**
 * Экземпляр модели контекста vk.
 * @param login Данные по логину.
 * @param tgInfo id цели размещения на стороне вконтакта. Резолвится через API utils.resolveScreenName.
 */
case class VkCtx(
  login   : Option[VkLoginResult],
  tgInfo  : Option[VkTargetInfo] = None
) extends IToJsonDict {

  /** Сериализация в JSON (js.Dynamic). */
  override def toJson: js.Dictionary[js.Any] = {
    val d = js.Dictionary.empty[js.Any]
    if (login.isDefined)
      d.update(LOGIN_FN, login.get.toJson)
    if (tgInfo.isDefined)
      d.update(TG_INFO_FN, tgInfo.get.toJson)
    d
  }
}
