package io.suggest.sc.hdr.v

import io.suggest.sc.hdr.m.HRightBtnClick
import io.suggest.sc.styl.ScCss.scCss

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 16:56
  * Description: Компонент кнопки, указывающей вправо (или "вперёд").
  */
object RightR extends HdrBtn {

  override protected[this] def _compName = "Right"

  override protected[this] def cssStyle = scCss.Header.Buttons.right

  override protected[this] def _btnClickAction = HRightBtnClick

  override protected[this] def svgPath: String = {
    """M14.26 10.3c.235 0 .475.083.665.252l7.55 6.702c.214.188.336.46.336.748s-.12.56-.334.748l-7.55 6.703c-.412.364-1.043.33-1.412-.085-.366-.412-.33-1.046.084-1.412L20.306 18l-6.708-5.954c-.413-.366-.45-.998-.084-1.41.196-.223.473-.338.748-.338z"""
  }

}
