package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sjs.common.vm.VmT
import io.suggest.sc.ScConstants.Block.ID_DELIM
import io.suggest.sc.ScConstants.Focused.FAd.BLOCK_ID_SUFFIX
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.util.{DomIdSuffix, DynDomIdRawString}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:57
 * Description: vm для sm-block внутри focused ad верстки.
 * Аналог модели GBlock для focused-карточек.
 */
object FAdBlock
  extends FindElDynIdT
    with DynDomIdRawString
    with DomIdSuffix
{

  override def DOM_ID_SUFFIX  = ID_DELIM + BLOCK_ID_SUFFIX
  override type Dom_t         = HTMLDivElement
  override type T             = FAdBlock

}


import FAdBlock.Dom_t


trait FAdBlockT extends VmT {
  override type T = Dom_t
}


case class FAdBlock(
  override val _underlying: Dom_t
)
  extends FAdBlockT
