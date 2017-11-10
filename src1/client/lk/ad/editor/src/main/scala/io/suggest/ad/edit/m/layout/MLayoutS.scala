package io.suggest.ad.edit.m.layout

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.10.17 17:07
  * Description: Модель состояния самого интерфейса редактора.
  * Появилась из-за необходимости равнять правую панель по шапке страницы на основе данных скроллинга.
  */
object MLayoutS {

  def empty = apply()

  implicit def univEq: UnivEq[MLayoutS] = UnivEq.derive

  /** Поддержка FastEq для инстансов [[MLayoutS]]. */
  implicit object MLayoutSFastEq extends FastEq[MLayoutS] {
    override def eqv(a: MLayoutS, b: MLayoutS): Boolean = {
      a.rightPanelY ===* b.rightPanelY
    }
  }

}


/** Класс модели состояния layout'а всея редактора.
  *
  * @param rightPanelY Абсолютная Y-координата панели редакторов.
  */
case class MLayoutS(
                     rightPanelTop  : Int              = 90,
                     rightPanelY    : Option[Int]      = None
                   ) {

  def withRightPanelY(rightPanelY: Option[Int]) = copy(rightPanelY = rightPanelY)

}
