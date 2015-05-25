package io.suggest.sc.sjs.v.welcome

import io.suggest.adv.ext.model.im.{ISize2di, Size2di}
import io.suggest.sc.sjs.c.NodeWelcomeCtl
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mv.IVCtx
import io.suggest.sc.sjs.m.mwc.MWelcomeState
import io.suggest.sc.sjs.v.vutil.{VImgUtil, VUtil}
import VUtil.getElementById
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import io.suggest.sjs.common.view.safe.evtg.SafeEventTarget
import org.scalajs.dom
import org.scalajs.dom.{Element, Event}
import org.scalajs.dom.raw.{HTMLImageElement, HTMLDivElement}
import io.suggest.sc.ScConstants.Welcome._
import io.suggest.sc.sjs.v.vutil.ExtraStyles._
import io.suggest.sc.ScConstants.CssAnim

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 10:45
 * Description: Представление управления приветствием узла выдачи.
 * welcome карточка удаляется из DOM, после того как скрыта.
 */
object NodeWelcomeView {

  /** Используемая для fade-out анимация. Заливается предварительно в will-change для ускорения браузера. */
  def FADEOUT_ANIM_TYPE = "opacity"

  /** Найти и вернуть div-контейнер карточки приветствия. */
  def rootDiv()  = getElementById[HTMLDivElement](ROOT_ID)
  def bgImg()    = getElementById[HTMLImageElement](BG_IMG_ID)
  def fgImg()    = getElementById[HTMLImageElement](FG_IMG_ID)
  def fgInfo()   = getElementById[HTMLDivElement](FG_INFO_DIV_ID)


  /**
   * Дедубликация вложенности и повторяющегося кода между fitBgImg() и fitFgImg().
   * @param imgOpt Опциональный результат bgImg() или fgImg().
   * @param f Функция обработки картинки и её data-размера.
   */
  private def _processImgWrap(imgOpt: Option[HTMLImageElement])(f: (HTMLImageElement, ISize2di) => Unit): Unit = {
    imgOpt.foreach { el =>
      VImgUtil.readDataWh(el) foreach { iwh =>
        f(el, iwh)
      }
    }
  }


  /** Подогнать фон приветствия под экран.
    * В оригинале было fit(imgDom, isDivided = false). */
  def fitBg() = _processImgWrap(bgImg()) { (el, iwh) =>
    val wndWh = MAgent.availableScreen
    val newWh = if (iwh.width / iwh.height < wndWh.width / wndWh.height) {
      val w = wndWh.width
      val h = w * iwh.height / iwh.width
      Size2di(w, height = h)
    } else {
      val h = wndWh.height
      val w = h * iwh.width / iwh.height
      Size2di(w, height = h)
    }
    val marginTopPx = - newWh.height / 2
    setImageWh(el, newWh, marginTopPx)
  }


  /** Подогнать передней план приветствия под экран. */
  def fitFg() = _processImgWrap(fgImg()) { (el, iwh) =>
    val newWidth  = iwh.width  / 2
    val newHeight = iwh.height / 2
    val newSz = Size2di(newWidth, height = newHeight)
    val marginTopPx = - (newHeight + 50) / 2
    setImageWh(el, newSz, marginTopPx)
    // Отработать текст/логотип переднего плана.
    fgInfo().foreach { fg =>
      fg.style.marginTop = (newHeight / 2) + "px"
    }
  }


  /** Вписать все элементы карточки приветствия под экран. */
  def fit(): Unit = {
    fitBg()
    fitFg()
  }


  /** Выставить новые отображаемые размеры для картинки и margin-left. */
  private def setImageWh(el: HTMLImageElement, wh: ISize2di, marginTopPx: Int): Unit = {
    VImgUtil.setImageWh(el, wh)
    el.style.marginLeft = (-wh.width / 2) + "px"
    el.style.marginTop  = marginTopPx + "px"
  }


  /**
   * Welcome-карточка ВОЗМОЖНО присутствует в DOM. Если присутствует, то значит отображена.
   * Нужно допилить карточку под экран, задать правила для сокрытия этой карточки через таймер или иные события.
   */
  def handleWelcome()(implicit vctx: IVCtx): Unit = {
    rootDiv().foreach { rootEl =>
      // Есть карточка в DOM. Подогнать по экран, повесить события.
      fit()

      // Добавляем will-change, т.к. ожидается анимация.
      rootEl.style.willChange = FADEOUT_ANIM_TYPE

      // Запустить скрытие карточки по таймауту.
      dom.setTimeout(
        { () => NodeWelcomeCtl.displayTimeout(rootEl) },
        MWelcomeState.HIDE_TIMEOUT_MS
      )

      // Вешаем события ускоренного ухода с приветствия.
      val safeEvtTg = SafeEventTarget(rootEl)
      safeEvtTg.addEventListener("click") { (evt: Event) =>
        NodeWelcomeCtl.clicked(evt, rootEl)
      }

      // TODO Нужно реагировать на "смахивание" приветствия.
      // TODO Реагировать на Esc/enter/etc на клавиатуре, как на педалирование анимации.
    }
  }


  /** Активна ли анимация прямо сейчас? */
  def isAnimatedNow(el: SafeCssElT): Boolean = {
    el.containsClass(CssAnim.TRANS_02_CSS_CLASS)
  }


  /** Запустить сокрытие с помощью css-анимации. И удалить потом ещё. */
  def fadeOut(el: SafeCssElT): Unit = {
    el.addClasses(CssAnim.TRANS_02_CSS_CLASS, CssAnim.FADEOUT_CSS_CLASS)
    dom.setTimeout(
      {() => removeWelcome(el._underlying) },
      MWelcomeState.FADEOUT_TRANSITION_MS
    )
  }

  /** Исполнить удаление элемента. */
  def removeWelcome(el: Element): Unit = {
    val parent = el.parentNode
    // Родительский элемент может быть null, если элемент уже удален.
    if (parent != null)
      parent.removeChild(el)
  }

}
