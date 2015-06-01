package io.suggest.sc.sjs.v.grid

import io.suggest.sc.sjs.m.mgrid.ICwCm
import io.suggest.sc.sjs.m.msrv.ads.find.MFoundAdJson
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.model.dom.DomListIterator
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:15
 * Description: view плитки рекламный карточек. Он получает команды от контроллеров или других view'ов,
 * поддерживая тем самым состояние отображаемой плитки карточек.
 */
object GridView {

  /**
   * Выставить новый размер контейнера сетки.
   * @param sz Данные по новому размеру.
   */
  def setContainerSz(sz: ICwCm, containerDiv: HTMLDivElement, loaderDivOpt: Option[HTMLDivElement]): Unit = {
    val width = sz.cw.toString + "px"

    containerDiv.style.width    = width
    containerDiv.style.left     = (sz.cm/2).toString + "px"
    containerDiv.style.opacity  = "1"

    loaderDivOpt.foreach { loader =>
      loader.style.width  = width
    }
  }


  /**
   * Залить переданные карточки в контейнер отображения.
   * @param containerDiv Контейнер карточек.
   * @param mads Рекламные карточки.
   * @return Контейнер с этой пачкой карточек.
   */
  def appendNewMads(containerDiv: HTMLDivElement, mads: TraversableOnce[MFoundAdJson]): HTMLDivElement = {
    // Склеить все отрендеренные карточки в одну html-строку. И распарсить пачкой.
    // Надо парсить и добавлять всей пачкой из-за особенностей браузеров по параллельной загрузке ассетов:
    // https://bugzilla.mozilla.org/show_bug.cgi?id=893113 -- Firefox: innerHTML может блокироваться на загрузку картинки.
    // Там в комментах есть данные по стандартам и причинам синхронной загрузки.
    val blocksHtmlSingle: String = {
      mads.toIterator
        .map(_.html)
        .reduceLeft { _ + _  }
    }
    val frag = VUtil.newDiv()
    frag.innerHTML = blocksHtmlSingle
    // Заливаем распарсенные карточки на страницу.
    containerDiv.appendChild(frag)
    frag
  }

  def rightBeforeBlockMoving(el: HTMLDivElement): Unit = {
    el.style.opacity = "1"
    el.style.display = "block"
  }

  /**
   * Двинуть блок на экране в указанные координаты. С помощью анимации, если возможно.
   * @param leftPx x-координата.
   * @param topPx y-координата.
   * @param el Элемент блока.
   * @param cssPrefixes Задетекченные css-префиксы.
   */
  def moveBlock(leftPx: Int, topPx: Int, el: HTMLDivElement, cssPrefixes: List[String]): Unit = {
    if (cssPrefixes.nonEmpty) {
      // Браузер умеет 3d-трансформации.
      val suf = "transform"
      val value = "translate3d(" + leftPx + "px," + topPx + "px,0)"
      for (cssPrefix <- cssPrefixes) {
        val prop = if (!cssPrefix.isEmpty) cssPrefix + suf else suf
        el.style.setProperty(prop, value)
      }

    } else {
      // Браузер не поддерживает трансформации. Отпозиционировать по хардкору:
      el.style.top  = topPx + "px"
      el.style.left = leftPx + "px"
    }
  }

}
