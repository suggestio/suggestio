package io.suggest.sc.m.dev

import diode.FastEq
import io.suggest.dev.MScreenInfo
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
      (a.info ===* b.info) &&
        (a.rszTimer ===* b.rszTimer)
    }
  }

  implicit def univEq: UnivEq[MScScreenS] = UnivEq.derive

}


/** Класс модели состояния экрана.
  *
  * @param info Описание текущего экрана устройства, в котором отрендерено приложение.
  * @param rszTimer Таймер уведомления других контроллеров о наступившем ресайзе.
  *                 Используется таймер задержки реакции для подавление повторных ресайзов.
  * @param safeAreaTopLeft Сдвиг левого верхнего угла для safe-area.
  *                        Для iphone10+ и возможно других устройст, к которых экран "вырезки" с краёв.
  */
case class MScScreenS(
                       info             : MScreenInfo,
                       rszTimer         : Option[Int]       = None
                     ) {

  def withInfo(info: MScreenInfo)           = copy(info = info)
  def withRszTimer(rszTimer: Option[Int])   = copy(rszTimer = rszTimer)

}
