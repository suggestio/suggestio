package io.suggest.lk.popup

import io.suggest.sjs.common.view.CommonPage
import org.scalajs.dom
import org.scalajs.jquery.{jQuery}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 18:01
 * Description: Система управления попапами в личном кабинете.
 * В оригинале (mx_cof) компонент назывался CbcaPopup.
 */
object Popup {

  /** id таргета для рендера попапов. */
  def CONTAINER_ID = "popupsContainer"

  /** Имя css-класса для покрытия фона оверлеем. */
  def OVERLAY_CSS_CLASS = "ovh"
  
  def container = jQuery("#" + CONTAINER_ID)

  /** Управление оверлеем. */
  def showHideOverlay(isShow: Boolean): Unit = {
    val body = jQuery( CommonPage.body )
    if (isShow)
      body.addClass(OVERLAY_CSS_CLASS)
    else
      body.removeClass(OVERLAY_CSS_CLASS)

    val visibility = if (isShow) "visible" else "hidden"
    val cont = container
    cont.css("visibility", visibility)

    val wnd = jQuery(dom.window)
    if (wnd.width() <= 1024) {
      if (isShow)
        cont.show()
      else
        cont.hide()
    }
  }

  /** Отображить оверлей. */
  def showOverlay() = showHideOverlay(isShow = true)
  /** Скрыть оверлей. */
  def hideOverlay() = showHideOverlay(isShow = false)

  

}
