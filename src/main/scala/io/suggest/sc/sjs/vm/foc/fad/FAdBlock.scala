package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sjs.common.vm.VmT
import io.suggest.sc.ScConstants.Block.ID_DELIM
import io.suggest.sc.ScConstants.Focused.FAd.BLOCK_ID_SUFFIX
import io.suggest.sjs.common.vm.find.{FindElPrefixedIdT, IApplyEl}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:57
 * Description: vm для sm-block внутри focused ad верстки.
 * Аналог модели GBlock для focused-карточек.
 */
object FAdBlock extends FindElPrefixedIdT with IApplyEl {

  override def DOM_ID = ID_DELIM + BLOCK_ID_SUFFIX
  override type Dom_t = HTMLDivElement
  override type T = FAdBlock

}


trait FAdBlockT extends VmT {
  override type T = HTMLDivElement
}


case class FAdBlock(
  override val _underlying: HTMLDivElement
)
  extends FAdBlockT
