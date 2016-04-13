package io.suggest.sc.sjs.vm.grid

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.create.{CreateDiv, CreateVm}
import io.suggest.sjs.common.vm.find.IApplyEl
import io.suggest.sjs.common.vm.of.{ChildrenVms, OfDiv}
import io.suggest.sjs.common.vm.walk.PrevNextSiblingsVmT
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.06.15 12:08
 * Description: Карточки в DOM заливаются пакетно. Тут модель одного такого пакета, который является div'ом.
 */

object GContainerFragment
  extends IApplyEl
    with CreateDiv
    with CreateVm
    with OfDiv
{

  override type Dom_t = HTMLDivElement
  override type T = GContainerFragment

  /**
   * Собрать новый фрагмент из внутреннего HTML будущего фрагмента.
   * @param innerHtml Внутренняя верстка нового фрагмента (верстка блоков).
   * @return Фрагмент, не привязанный к DOM.
   */
  def apply(innerHtml: String): GContainerFragment = {
    val vm = createNew()
    vm.setContent( innerHtml )
    vm
  }

}


import GContainerFragment.Dom_t


/** Логика экземпляров модели здесь. */
trait GContainerFragmentT
  extends VmT
    with PrevNextSiblingsVmT
    with SetInnerHtml
    with ChildrenVms
{

  override type T = Dom_t
  override type Self_t <: GContainerFragmentT

  /** Итератор блоков, содержащихся в данном контейнере. */
  def blocksIterator = _childrenVms

  override type ChildVm_t = GBlock
  override protected def _childVmStatic = GBlock

  /** Список блоков, содержащихся в этом фрагменте. */
  def blocks = blocksIterator.toList

  private def _firstLastBlkHelper(f: T => Node): Option[GBlock] = {
    Option( f(_underlying) )
      .flatMap { GBlock.ofNodeUnsafe }
  }
  def firstBlock = _firstLastBlkHelper(_.firstChild)
  def lastBlock  = _firstLastBlkHelper(_.lastChild)

}


/** Дефолтовая реализация экземпяра модели [[GContainerFragmentT]]. */
case class GContainerFragment(override val _underlying: Dom_t)
  extends GContainerFragmentT {

  override lazy val blocks = super.blocks
  override type Self_t = GContainerFragment
  override protected def _companion = GContainerFragment
}

