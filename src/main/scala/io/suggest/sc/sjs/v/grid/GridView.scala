package io.suggest.sc.sjs.v.grid

import io.suggest.sc.sjs.c.GridCtl
import io.suggest.sc.sjs.m.mgrid.{MBlockInfo, ICwCm}
import io.suggest.sc.sjs.m.msrv.ads.find.MFoundAdJson
import io.suggest.sc.sjs.v.vutil.{OnClick, SetStyleDisplay, VUtil}
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:15
 * Description: view плитки рекламный карточек. Он получает команды от контроллеров или других view'ов,
 * поддерживая тем самым состояние отображаемой плитки карточек.
 */
object GridView extends SetStyleDisplay with OnClick {

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

  def setContainerHeight(h: Int, containerDiv: HTMLDivElement): Unit = {
    containerDiv.style.height = h + "px"
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

  /**
   * Двинуть блок на экране в указанные координаты. С помощью анимации, если возможно.
   * @param leftPx x-координата.
   * @param topPx y-координата.
   * @param el Элемент блока.
   * @param cssPrefixes Задетекченные css-префиксы.
   */
  def moveBlock(leftPx: Int, topPx: Int, el: HTMLDivElement, cssPrefixes: List[String], withAnim: Boolean = true): Unit = {
    //el.style.opacity = "1"
    if (withAnim && cssPrefixes.nonEmpty) {
      displayBlock(el)
      // Браузер умеет 3d-трансформации.
      val suf = "transform"
      // translate3d(+x, +y) работает с относительными координатами. Надо поправлять их с учетом ВОЗМОЖНЫХ значений style.top и style.left.
      val leftPx1 = _fixRelCoord(el.style.left, leftPx)
      val topPx1  = _fixRelCoord(el.style.top,  topPx)
      val value = "translate3d(" + leftPx1 + "px," + topPx1 + "px,0)"
      for (cssPrefix <- cssPrefixes) {
        val prop = if (!cssPrefix.isEmpty) cssPrefix + suf else suf
        el.style.setProperty(prop, value)
      }

    } else {
      // Анимация отключена. Отпозиционировать по хардкору:
      el.style.top  = topPx + "px"
      el.style.left = leftPx + "px"
      displayBlock(el)
    }
  }

  /**
   * Полностью очистить сетку от карточек.
   * @param containerDiv контейнер сетки.
   */
  def clear(containerDiv: HTMLDivElement): Unit = {
    VUtil.removeAllChildren(containerDiv)
  }

  /**
   * Внести поправку в указанную абсолютную координату с помощью строковых данных по имеющейся относительной.
   * @param src Исходная строка, содержащая абсолютную координату.
   * @param abs Целевая абсолютная координата.
   * @return Новая относительная координата на основе abs и возможного значения из src.
   */
  private def _fixRelCoord(src: String, abs: Int): Int = {
    DataUtil.extractInt(src)
      .fold(abs)(abs - _)
  }


  /**
   * Инициализировать новые блоки: повесить события.
   * @param blocks Новые блоки.
   */
  def initNewBlocks(blocks: TraversableOnce[MBlockInfo]): Unit = {
    for (b <- blocks) {
      val safe = SafeEl( b.block )
      onClick(safe) { e: Event =>
        GridCtl.onBlockClick(b, e)
      }
    }
  }

}
