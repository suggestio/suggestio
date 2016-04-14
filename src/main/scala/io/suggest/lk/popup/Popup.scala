package io.suggest.lk.popup

import io.suggest.err.ErrorConstants
import io.suggest.sjs.common.controller.{DomQuick, InitRouter}
import io.suggest.sjs.common.controller.jshidden.JsHidden
import io.suggest.sjs.common.controller.vlines.VerticalLines
import io.suggest.sjs.common.util.{SjsLogger, TouchUtil}
import io.suggest.sjs.common.vm.doc.DocumentVm
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery}
import io.suggest.popup.PopupConstants._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.concurrent.Future
import scala.scalajs.js.Any

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 18:01
 * Description: Система управления попапами в личном кабинете.
 * В оригинале (mx_cof) компонент назывался CbcaPopup.
 */
object Popup extends SjsLogger {

  def containerSelector =  "#" + CONTAINER_ID
  def container = jQuery(containerSelector)

  def allPopups(cont: JQuery = container): JQuery = {
    cont.children("." + POPUP_CLASS)
  }

  private def getWnd = jQuery(dom.window)

  def isWindowNarrow(wnd: JQuery): Boolean = {
    wnd.width() <= 1024
  }

  /** Управление оверлеем. */
  def showHideOverlay(isShow: Boolean): Unit = {
    val body = jQuery( DocumentVm().body )
    if (isShow)
      body.addClass(OVERLAY_CSS_CLASS)
    else
      body.removeClass(OVERLAY_CSS_CLASS)

    val visibility = if (isShow) "visible" else "hidden"
    val cont = container
    cont.css("visibility", visibility)

    val wnd = getWnd
    if (isWindowNarrow(wnd)) {
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
    popups.each { (index: Int, el: Element) =>
      val popup = jQuery(el)
      val pheight = popup.height()
      val cheight = container.height()
      val hdiff = cheight - pheight

      val minTop = MIN_TOP_PX
      val top: Int = {
        if (hdiff > minTop * 2  &&  getWnd.width() > 767) {
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

  /** Добавить новый попап в контейнер попапов. */
  def appendPopup(content: Any): Unit = {
    container.append(content)
  }

  /** Показать указанные попапы. */
  def showPopups(selector: String): Unit = {
    _contJqSel(selector)(showPopups)
  }
  /** Показать выбранные попапы. */
  def showPopups(popups: JQuery): Unit = {
    popups.show()
    JsHidden.processAllIn(popups)
    VerticalLines.resetVLinesHeightsIn(popups)
    // Если есть картинки, то надо отсрочить реальное отображение попапа до загрузки оных. TODO Точно надо?
    val images = popups.find("img")
    // Дублирующийся код заворачиваем в замыкание:
    def _showThisPopups(): Unit = {
      showOverlay()
      resetPosition(popups)
    }
    // Выполнить отображение попапа на экран (сейчас или отложенно).
    if (images.length > 0) {
      images.on("load", { (e: JQueryEventObject) =>
        _showThisPopups()
      })
    } else {
      _showThisPopups()
    }
    // Проскроллить страницу наверх на нешироких экранах. TODO Правильно ли это? Может надо "скроллить" позицию попапа?
    val wnd = getWnd
    if ( isWindowNarrow(wnd) )
      wnd.scrollTop(0)
  }


  /** На телефонах нужно иначе реагировать на скроллинг. */
  private def phoneScroll(): Unit = {
    val wnd = getWnd
    if (isWindowNarrow(wnd)) {
      // В оригинале был тут ещё $('.overflow-scrolling').height(windowHeight). Но этот класс канул в лету давно.
      container.css("min-height", wnd.height() + 1)
    }
  }


  /** Скрыть все попапы. */
  def hideAllPopups(cont: JQuery = container): Unit = {
    hidePopups(allPopups(cont))
  }
  /** Сокрытие попапов по селектору. */
  def hidePopups(selector: String): Unit = {
    _contJqSel(selector)(hidePopups)
  }
  /** Сокрытие указанных попапов. */
  def hidePopups(popups: JQuery): Unit = {
    hideOverlay()
    popups.hide()
    // Закрытие клавиатуры на мобильном устройстве: сбросить фокус у полей ввода. Не ясно только, почему оно здесь лежит...
    if (isWindowNarrow(getWnd)) {
      jQuery("input").blur()
    }
    // В оригинале тут ещё был вызов $('#overlayData').hide(), но этот класс давно выпилен.
  }

  /** Инициализация подсистемы попапов. */
  def init(): Unit = {
    val cont = container
    if (cont.length > 0) {
      _doInit(cont)
    } else {
      warn("Popups init() requested, but no " + containerSelector + " found.")
    }
  }

  /** Непосредственная инициализация. */
  private def _doInit(cont: JQuery): Unit = {
    hideOverlay()
    phoneScroll()

    val wnd = getWnd
    wnd.resize { e: JQueryEventObject =>
      resetPositions()
      phoneScroll()
      // В оригинале был scrollTop для всех случаев.
      if (isWindowNarrow(wnd))
        wnd.scrollTop(0)
    }

    val clickEvent = TouchUtil.clickEvtNamesJq

    // Скрывать попап при нажатии кнопки закрытия попапа.
    cont.on(clickEvent, "." + CLOSE_CSS_CLASS, { e: JQueryEventObject =>
      e.preventDefault()
      val el = jQuery( e.currentTarget )
      val popup = el.parent("." + POPUP_CLASS)
      hidePopups(popup)
    })

    // При клике по подложке попапов скрывать попапы.
    cont.on(clickEvent, { e: JQueryEventObject =>
      hideAllPopups(cont)
    })
    // Гасить события закрытия при кликам, если они внутри попапа.
    cont.on(
      clickEvent,
      "." + POPUP_CLASS + ", ." + JS_POPUP_CLASS,
      {e: JQueryEventObject => e.stopPropagation() }
    )

    // Если после перезагрузки страницы в попапе есть поля с ошибками, нужно его отобразить
    allPopups(cont)
      .has("." + ErrorConstants.FORM_CSS_CLASS + ", ." + ErrorConstants.ERROR_MSG_CLASS)
      .each { (index: Int, el: Element) =>
        val jqel = jQuery(el)
        val popup = jqel.parent("." + POPUP_CLASS)
        showPopups(popup)
          .asInstanceOf[Any]
      }

    // Какие-то попапы надо не только скрывать, но и удалять по клику на соотв. элементу.
    cont.on(clickEvent, "." + REMOVE_CSS_CLASS, {e: JQueryEventObject =>
      val el = jQuery(e.currentTarget)
      val popup = el.parent("." + POPUP_CLASS)
      hidePopups(popup)
      popup.remove()
    })

    // Рекция на кнопку ESC
    jQuery(dom.document).keyup { (e: JQueryEventObject) =>
      if (e.which == 27) {
        hideAllPopups(cont)
      }
    }

    // фикс отступа, который появляется при скрытии клавиатуры в открытом попапе    // TODO нужно ли?
    cont.on("blur", "." + JS_POPUP_CLASS + " input", { e: JQueryEventObject =>
      val listener = {() =>
        val activeInputs = cont.find("input:focus")
        if (activeInputs.length == 0) {
          wnd.scrollTop(0)
        }
      }
      DomQuick.setTimeout(100)(listener)
    })
  }

}

/** Аддон для роутера инициализации для активации системы попапов. */
trait PopupsInitRouter extends InitRouter {
  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.Popups) {
      Future {
        Popup.init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }
}
