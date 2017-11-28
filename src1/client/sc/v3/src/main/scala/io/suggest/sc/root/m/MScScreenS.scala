package io.suggest.sc.root.m

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
      a.screen ===* b.screen
    }
  }

  implicit def univEq: UnivEq[MScScreenS] = UnivEq.derive

}


/** Класс модели состояния экрана.
  *
  * @param screen Описание текущего экрана устройства, в котором отрендерено приложение.
  */
case class MScScreenS(
                       screen      : MScreen
                     ) {

  def withScreen(screen: MScreen) = copy(screen = screen)

}
