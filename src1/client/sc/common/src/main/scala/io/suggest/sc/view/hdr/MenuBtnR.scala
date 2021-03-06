package io.suggest.sc.view.hdr

import io.suggest.common.empty.OptionUtil
import io.suggest.sc.model.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.view.styl.ScCssStatic

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 15:44
  * Description: Компонент кнопки меню.
  */
class MenuBtnR extends HdrBtn {

  override protected[this] def cssStyle = ScCssStatic.Header.Buttons.menu

  override protected[this] def _btnClickAction = SideBarOpenClose(MScSideBars.Menu, open = OptionUtil.SomeBool.someTrue)

  override protected[this] def svgPath = {
    """M28.036 13.165h-20c-.552 0-1-.448-1-1 0-.553.448-1 1-1h20c.553 0 1 .447 1 1 0 .552-.447 1-1 1zM28.036 17.054h-20c-.552 0-1-.448-1-1 0-.553.448-1 1-1h20c.553 0 1 .447 1 1 0 .55-.447 1-1 1zM28.036 20.942h-20c-.552 0-1-.447-1-1s.448-1 1-1h20c.553 0 1 .447 1 1s-.447 1-1 1zM28.036 24.83h-20c-.552 0-1-.446-1-1s.448-1 1-1h20c.553 0 1 .448 1 1s-.447 1-1 1z"""
  }

}
