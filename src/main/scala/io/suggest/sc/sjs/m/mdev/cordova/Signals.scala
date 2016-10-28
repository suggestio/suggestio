package io.suggest.sc.sjs.m.mdev.cordova

import io.suggest.sjs.common.fsm.signals.IVisibilityChangeSignal
import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 16:03
  * Description: Сигналы cordova runtime.
  */
trait ICordovaFsmMsg extends IFsmMsg

/** Приложение отправляется в фон. */
case class Pause(event: Event)
  extends ICordovaFsmMsg
  with IVisibilityChangeSignal
{
  override def isHidden: Boolean = true
}
object Pause
  extends IFsmEventMsgCompanion


/** Восстановление активности приложения. */
case class Resume(event: Event)
  extends ICordovaFsmMsg
  with IVisibilityChangeSignal
{
  override def isHidden = false
}
object Resume
  extends IFsmEventMsgCompanion


/** Сигнал нажатия клавиши меню. */
case class MenuButton(event: Event)
  extends ICordovaFsmMsg
object MenuButton
  extends IFsmEventMsgCompanion
