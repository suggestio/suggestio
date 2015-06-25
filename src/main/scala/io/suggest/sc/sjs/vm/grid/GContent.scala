package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.ScConstants.Grid
import io.suggest.sc.sjs.m.mgrid.ICwCm
import io.suggest.sc.sjs.vm.util.CssSzImplicits
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.get.ChildElOrFindInner
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 16:10
 * Description: Модель grid content div.
 * Этот div содержит основной контент grid'а в виде div'ов.
 */
object GContent extends FindDiv {

  override type T = GContent
  override def DOM_ID: String = Grid.CONTENT_DIV_ID

}


/** Логика фунцкионирования экземпляра модели вынесена сюда. */
trait GContentT extends SafeElT with ChildElOrFindInner with CssSzImplicits {

  override type T = HTMLDivElement

  /** Доступ к ViewModel'и loader'а плитки. */
  def loader: Option[GLoader] = {
    val t = new ChildFinderT {
      override type SubTagVm_t = GLoader.T
      override protected type SubTagEl_t = GLoader.Dom_t
      override protected def _subtagCompanion = GLoader
    }
    t._findSubtag()
  }

  /** Доступ к модели контейнера карточек. */
  def container: Option[GContainer] = {
    val t = new ChildFinderT {
      override type SubTagVm_t = GContainer.T
      override protected type SubTagEl_t = GContainer.Dom_t
      override protected def _subtagCompanion = GContainer
    }
    t._findSubtag()
  }

  /**
   * Выставить параметры контейнера.
   * @param sz Вычисленные размеры контейнера.
   */
  def setContainerSz(sz: ICwCm): Unit = {
    val widthCss = sz.cw.px
    for (l <- loader) {
      l._setWidthPx(widthCss)
    }
    for (c <- container) {
      c._setContainerSz(widthCss, sz.cm)
    }
  }

}


/**
 * Экземпляр модели grid content div.
 * @param _underlying DOM-элемент, соответствующий экземпляру.
 */
case class GContent(override val _underlying: HTMLDivElement)
  extends GContentT {

  override lazy val loader = super.loader
  override lazy val container = super.container
}
