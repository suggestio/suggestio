package io.suggest.sc.sjs.vm.grid

import io.suggest.common.css.CssSzImplicits
import io.suggest.sc.ScConstants.Grid
import io.suggest.sc.sjs.m.mgrid.IGridData
import io.suggest.sc.sjs.m.msrv.ads.find.MFoundAdJson
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.content.ClearT
import io.suggest.sjs.common.vm.child.ContentElT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.StyleHeight
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
trait GContainerT extends ContentElT with CssSzImplicits with ClearT with StyleHeight {

  override type T = HTMLDivElement

  /** Внутреннее межмодельное API для согласования параметров отображения div'ов. */
  protected[grid] def _setContainerSz(widthCss: String, cm: Int): Unit = {
    val s = _underlying.style
    s.width    = widthCss
    s.left     = (cm/2).px
    s.opacity  = "1"
  }

  /**
   * Посчитать высоту контейнера на основе данных сетки и выставить её.
   * @param grid Источних данных сетки.
   */
  def resetHeightUsing(grid: IGridData): Unit = {
    val maxColHeight = grid.builderState.maxColHeight
    // TODO В оригинале maxColHeightPx почему-то перемножалось с paddedCellSize и не глючило ничего.
    // В новой выдаче это приводит к очевидному звиздецу по высоте, но в оригинале нет проблем.
    // Оригинал смотрать в showcase2.coffee по слову real_h.
    //val maxPxH = topOffset  +  paddedCellSize * maxCellH  +  bottomOffset
    val mgp = grid.params
    val maxColHeightPx = maxColHeight * mgp.paddedCellSize - mgp.cellPadding
    val totalHeightPx = mgp.topOffset  +  maxColHeightPx  +  mgp.bottomOffset
    setHeightPx(totalHeightPx)
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


  def fragmentsIterator: Iterator[GContainerFragment] = {
    DomListIterator( _underlying.children )
      .map { el => GContainerFragment( el.asInstanceOf[HTMLDivElement] ) }
  }

  def blocksIterator: Iterator[GBlock] = {
    fragmentsIterator
      .flatMap( _.blocksIterator )
  }

}


/** Дефолтовая реализация экземпляра модели на базе [[GContainerT]]. */
case class GContainer(
  override val _underlying: HTMLDivElement
) extends GContainerT
