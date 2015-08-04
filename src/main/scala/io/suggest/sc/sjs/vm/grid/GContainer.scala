package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.ScConstants.Grid
import io.suggest.sc.sjs.m.mgrid.{IGridData, MGridParams}
import io.suggest.sc.sjs.m.msrv.ads.find.MFoundAdJson
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.CssSzImplicits
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 16:50
 * Description: Модель контейнера карточек (блоков) плитки.
 */
object GContainer extends FindDiv {
  override type T = GContainer
  override def DOM_ID = Grid.CONTAINER_DIV_ID
}


/** Логика модели вынесена в отдельный трейт. */
trait GContainerT extends ISafe with CssSzImplicits {

  override type T = HTMLDivElement

  /** Внутреннее межмодельное API для согласования параметров отображения div'ов. */
  protected[grid] def _setContainerSz(widthCss: String, cm: Int): Unit = {
    val s = _underlying.style
    s.width    = widthCss
    s.left     = (cm/2).px
    s.opacity  = "1"
  }

  /**
   * Сеттер высоты контейнера.
   * @param h Высота в пикселях.
   */
  def setHeightPx(h: Int): this.type = {
    setHeight(h.px)
  }

  /**
   * Посчитать высоту контейнера на основе данных сетки и выставить её.
   * @param grid Источних данных сетки.
   */
  def resetHeightUsing(grid: IGridData): Unit = {
    val maxColHeightPx = grid.builderState.maxColHeight
    // TODO В оригинале maxColHeightPx почему-то перемножалось с paddedCellSize и не глючило ничего.
    // В новой выдаче это приводит к очевидному звиздецу по высоте, но в оригинале нет проблем.
    // Оригинал смотрать в showcase2.coffee по слову real_h.
    //val maxPxH = topOffset  +  paddedCellSize * maxCellH  +  bottomOffset
    val mgp = grid.params
    val totalHeightPx = mgp.topOffset  +  maxColHeightPx  +  mgp.bottomOffset
    setHeightPx(totalHeightPx)
  }

  /** Выставить css-значение высоты для текущего контейнера. */
  def setHeight(cssHeight: String): this.type = {
    _underlying.style.height = cssHeight
    this
  }

  /**
   * Залить переданные сервером карточки в контейнер отображения.
   * @param mads Рекламные карточки.
   * @return Контейнер с этой пачкой карточек.
   */
  def appendNewMads(mads: TraversableOnce[MFoundAdJson]): GContainerFragment = {
    // Склеить все отрендеренные карточки в одну html-строку. И распарсить пачкой.
    // Надо парсить и добавлять всей пачкой из-за особенностей браузеров по параллельной загрузке ассетов:
    // https://bugzilla.mozilla.org/show_bug.cgi?id=893113 -- Firefox: innerHTML= может блокироваться на загрузку картинки.
    // Там в комментах есть данные по стандартам и причинам синхронной загрузки.
    val blocksHtmlSingle: String = {
      mads.toIterator
        .map(_.html)
        .mkString
    }
    val frag = GContainerFragment(blocksHtmlSingle)
    // Заливаем распарсенные карточки на страницу.
    _underlying.appendChild( frag._underlying )
    frag
  }


  /** Полностью очистить сетку от карточек. */
  def clear(): Unit = {
    VUtil.removeAllChildren(_underlying)
  }


  def fragmentsIterator: Iterator[GContainerFragment] = {
    DomListIterator( _underlying.children )
      .map { el => GContainerFragment( el.asInstanceOf[HTMLDivElement] ) }
  }

}


/** Дефолтовая реализация экземпляра модели на базе [[GContainerT]]. */
case class GContainer(
  override val _underlying: HTMLDivElement
) extends GContainerT
