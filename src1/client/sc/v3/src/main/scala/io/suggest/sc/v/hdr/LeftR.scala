package io.suggest.sc.v.hdr

import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.styl.GetScCssF

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 16:56
  * Description: Компонент кнопки, указывающей вправо (или "вперёд").
  */
class LeftR( getScCssF: GetScCssF ) extends HdrBtn {

  override protected[this] def cssStyle = getScCssF().Header.Buttons.leftCss

  override protected[this] def _btnClickAction = SideBarOpenClose(MScSideBars.Menu, open = false)

  override protected[this] def svgPath = {
    """M21.81 25.705c-.233 0-.474-.084-.663-.252l-7.55-6.703c-.214-.188-.336-.46-.336-.748s.123-.558.337-.748l7.55-6.703c.412-.365 1.043-.33 1.412.085.364.413.327 1.045-.085 1.412l-6.708 5.955 6.708 5.955c.412.366.45.998.084 1.412-.198.22-.473.335-.75.335z"""
  }

}
