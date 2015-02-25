package io.suggest.xadv.ext.js.vk.c.low

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 16:30
 * Description: sjs-интерфейс к статическому vk open api.
 */

/** Описываем глобально-доступный объект VK и его API. */
@JSName("VK")
object VkLow extends js.Object {
  def init(options: js.Dynamic): Unit = js.native
  def Api: VkApi = js.native
  def Auth: VkAuth = js.native
}


/** Интерфейс содержимого VK.Api. */
trait VkApi extends js.Object {
  def call(method: String, params: JSON, callback: Callback): Unit = js.native
}


/** Интерфейс содержимого VK.Auth. */
trait VkAuth extends js.Object {
  def login(callback: Callback): Unit = js.native
  def getLoginStatus(callback: Callback): Unit = js.native
}
