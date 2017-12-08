package io.suggest.sc.m.dev

import diode.FastEq
import io.suggest.dev.MScreen
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.17 10:57
  * Description: Модель состояния экрана.
  * Изначально просто жила внутри mroot.index.state, что было неправильно.
  */
object MScScreenS {

  /** Поддержка FastEq для инстансов [[MScScreenS]]. */
  implicit object MScScreenSFastEq extends FastEq[MScScreenS] {
    override def eqv(a: MScScreenS, b: MScScreenS): Boolean = {
      (a.screen ===* b.screen) &&
        (a.rszTimer ===* b.rszTimer)
    }
  }

  implicit def univEq: UnivEq[MScScreenS] = UnivEq.derive

}


/** Класс модели состояния экрана.
  *
  * @param screen Описание текущего экрана устройства, в котором отрендерено приложение.
  * @param rszTimer Таймер уведомления других контроллеров о наступившем ресайзе.
  *                 Используется таймер задержки реакции для подавление повторных ресайзов.
  */
case class MScScreenS(
                       screen      : MScreen,
                       rszTimer    : Option[Int] = None
                     ) {

  def withScreen(screen: MScreen)           = copy(screen = screen)
  def withRszTimer(rszTimer: Option[Int])   = copy(rszTimer = rszTimer)

}
