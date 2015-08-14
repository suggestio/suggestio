package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.domvm.IApplyEl
import io.suggest.sc.sjs.vm.util.domvm.walk.PrevNextSiblingsVmT
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.06.15 12:08
 * Description: Карточки в DOM заливаются пакетно. Тут модель одного такого пакета, который является div'ом.
 */

object GContainerFragment extends IApplyEl {

  override type Dom_t = HTMLDivElement
  override type T = GContainerFragment

  /**
   * Собрать новый фрагмент из внутреннего HTML будущего фрагмента.
   * @param innerHtml Внутренняя верстка нового фрагмента (верстка блоков).
   * @return Фрагмент, не привязанный к DOM.
   */
  def apply(innerHtml: String): GContainerFragment = {
    val div = VUtil.newDiv()
    div.innerHTML = innerHtml
    GContainerFragment(div)
  }

}


/** Логика экземпляров модели здесь. */
trait GContainerFragmentT extends SafeElT with PrevNextSiblingsVmT {

  override type T = HTMLDivElement
  override type Self_t <: GContainerFragmentT

  /** Итератор блоков, содержащихся в данном контейнере. */
  def blocksIterator: Iterator[GBlock] = {
    DomListIterator( _underlying.children )
      .flatMap { GBlock.fromNode }
  }

  /** Список блоков, содержащихся в этом фрагменте. */
  def blocks = blocksIterator.toList

  private def _firstLastBlkHelper(f: T => Node): Option[GBlock] = {
    Option( f(_underlying).asInstanceOf[GBlock.Dom_t] )
      .map { GBlock.apply }
  }
  def firstBlock = _firstLastBlkHelper(_.firstChild)
  def lastBlock  = _firstLastBlkHelper(_.lastChild)

}


/** Дефолтовая реализация экземпяра модели [[GContainerFragmentT]]. */
case class GContainerFragment(override val _underlying: HTMLDivElement)
  extends GContainerFragmentT {

  override lazy val blocks = super.blocks
  override type Self_t = GContainerFragment
  override protected def _companion = GContainerFragment
}

