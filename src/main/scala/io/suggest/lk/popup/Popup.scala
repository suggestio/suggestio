package io.suggest.lk.popup

import io.suggest.sjs.common.controller.vlines.VerticalLines
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.CommonPage
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.jquery.{JQuery, jQuery}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 18:01
 * Description: Система управления попапами в личном кабинете.
 * В оригинале (mx_cof) компонент назывался CbcaPopup.
 */
object Popup extends SjsLogger {

  /** Минимальный сдвиг по вертикали при позиционировании. */
  def MIN_TOP_PX = 25

  /** id таргета для рендера попапов. */
  def CONTAINER_ID = "popupsContainer"

  /** Имя css-класса для покрытия фона оверлеем. */
  def OVERLAY_CSS_CLASS = "ovh"

  def containerSelector =  "#" + CONTAINER_ID
  def container = jQuery(containerSelector)

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


  /** Сброс позиционирования для всех имеющихся попапов. Используется при инициализации. */
  def resetPositions(): Unit = {
    resetPosition(".popup:visible")
  }
  /** Перецентровка указанного попапа. */
  def resetPosition(selector: String): Unit = {
    _contJqSel(selector)(resetPosition)
  }
  /** Центровка всех переданных попапов. */
  def resetPosition(popups: JQuery): Unit = {
    popups.each { (index: Any, el: Element) =>
      val popup = jQuery(el)
      val pheight = popup.height()
      val cheight = container.height()
      val hdiff = cheight - pheight

      val minTop = MIN_TOP_PX
      val top: Int = {
        if (hdiff > minTop * 2  &&  jQuery(dom.window).width() > 767) {
          Math.ceil( hdiff.toDouble / 2.0 )
            .toInt
        } else {
          minTop
        }
      }
      popup.css("top", top)
    }
  }

  /** Добавить id контейнера попапов в селектор, если там нет id-селектора в начале. */
  private def _contSel(selector: String): String = {
    if (selector.startsWith("#")) {
      selector
    } else {
      containerSelector + " > " + selector
    }
  }

  private def _contJqSel[T](selector: String)(f: JQuery => T): T = {
    val jqSel = _contSel(selector)
    f(jQuery(jqSel))
  }


  /** Показать указанные попапы. */
  def showPopups(selector: String): Unit = {
    _contJqSel(selector)(showPopups)
  }
  /** Показать указанные попапы. */
  def showPopups(popups: JQuery): Unit = {
    popups.show()
    VerticalLines.resetVLinesHeightsIn(popups)
    // TODO hide elements и остальное
    ???
  }

}
