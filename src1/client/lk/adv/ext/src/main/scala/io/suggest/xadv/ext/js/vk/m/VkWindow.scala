package io.suggest.xadv.ext.js.vk.m

import org.scalajs.dom.Window

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 15:58
 * Description: vkontakte для асинхронной инициализации использует поле window.vkAsyncInit.
 * @see [[https://vk.com/pages?oid=-17680044&p=Open_API]] см.Asynchronized Initialization
 */
@js.native
@JSGlobal
class VkWindow extends Window {

  var vkAsyncInit: js.Function0[_] = js.native

}


object VkWindow {

  implicit def w2vkw(w: Window): VkWindow = {
    w.asInstanceOf[VkWindow]
  }

}
