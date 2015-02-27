package io.suggest.xadv.ext.js.vk.m

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

  def LOGIN_FN = "a"

  /** Десериализовать контекст. */
  def fromDyn(raw: js.Any): VkCtx = {
    val d = raw.asInstanceOf[JSON] : WrappedDictionary[js.Dynamic]
    VkCtx(
      login = d.get(LOGIN_FN).map(VkLoginResult.fromJson)
    )
  }


  /** Десериализовать опциональный контекст. */
  def maybeFromDyn(rawOpt: Option[js.Any]): Option[VkCtx] = {
    // Можно добавить сюда обработку ошибок.
    rawOpt map fromDyn
  }

}

import VkCtx._


/** Абстрактный экземпляр модели. */
trait VkCtxT {
  /** id цели размещения на стороне вконтакта. Резолвится через API utils.resolveScreenName. */
  def login: Option[VkLoginResult]

  /** Сериализация в JSON (js.Dynamic). */
  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    if (login.isDefined)
      lit.updateDynamic(LOGIN_FN)(login.get.toJson)
    lit
  }
}


case class VkCtx(
  login: Option[VkLoginResult]
) extends VkCtxT
