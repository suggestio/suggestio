package io.suggest.sc.sjs.m.mdev

import _root_.cordova.CordovaConstants
import io.suggest.common.event.VisibilityChangeEvents
import io.suggest.sjs.common.fsm.{IFsmMsg, SjsFsm}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 14:08
  * Description: Сообщение о подписке/отписке на platform-события.
  * @param event см. [[PlatformEvents]].
  * @param listener FSM, подписываемый на события.
  * @param subscribe true - подписать FSM, false - отписать его.
  */
case class PlatEventListen(
  event     : String,
  listener  : SjsFsm,
  subscribe : Boolean
)
  extends IFsmMsg


/** Контейнер унифицированных id событий, которые понимает [[io.suggest.sc.sjs.c.plat.PlatformFsm]]. */
object PlatformEvents extends VisibilityChangeEvents {

  def E_DEVICE_READY = CordovaConstants.Events.DEVICE_READY

  def MENU_BTN       = CordovaConstants.Events.MENU_BUTTON

}
