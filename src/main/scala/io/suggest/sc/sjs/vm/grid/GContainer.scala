package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.ScConstants.Grid
import io.suggest.sc.sjs.m.msrv.ads.find.MFoundAdJson
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.CssSzImplicits
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
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
    _underlying.style.width    = widthCss
    _underlying.style.left     = (cm/2).px
    _underlying.style.opacity  = "1"
  }

  /**
   * Сеттер высоты контейнера.
   * @param h Высота в пикселях.
   */
  def heightPx_=(h: Int): this.type = {
    _underlying.style.height = h.px
    this
  }

  /**
   * Залить переданные карточки в контейнер отображения.
   * @param mads Рекламные карточки.
   * @return Контейнер с этой пачкой карточек.
   */
  def appendNewMads(mads: TraversableOnce[MFoundAdJson]): HTMLDivElement = {
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
    _underlying.appendChild(frag)
    frag
  }


  /** Полностью очистить сетку от карточек. */
  def clear(): Unit = {
    VUtil.removeAllChildren(_underlying)
  }

}


/** Дефолтовая реализация экземпляра модели на базе [[GContainerT]]. */
case class GContainer(override val _underlying: HTMLDivElement) extends GContainerT
