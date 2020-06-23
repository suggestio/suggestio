package io.suggest.sc.v.hdr

import io.suggest.common.empty.OptionUtil
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.v.styl.ScCssStatic

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 13:45
  * Description: React-компонент для кнопки поиска на панели заголовка.
  * Для рендера используется inline SVG также как и в предыдущих версиях системы.
  */
class SearchBtnR extends HdrBtn {

  override protected[this] def cssStyle = ScCssStatic.Header.Buttons.search

  override protected[this] def _btnClickAction = SideBarOpenClose(MScSideBars.Search, open = OptionUtil.SomeBool.someTrue)

  override protected[this] def svgPath = {
    """M15.327 23.914c-4.825 0-8.75-3.925-8.75-8.75 0-4.824 3.925-8.75 8.75-8.75 4.826 0 8.75 3.926 8.75 8.75 0 4.825-3.924 8.75-8.75 8.75zm0-15.5c-3.722 0-6.75 3.028-6.75 6.75 0 3.723 3.028 6.75 6.75 6.75s6.75-3.027 6.75-6.75c0-3.722-3.027-6.75-6.75-6.75zM28.495 29.582c-.256 0-.512-.098-.707-.293l-5.5-5.5c-.39-.392-.39-1.024 0-1.415s1.023-.39 1.414 0l5.5 5.5c.39.39.39 1.023 0 1.414-.195.194-.45.292-.707.292z"""
  }

}
