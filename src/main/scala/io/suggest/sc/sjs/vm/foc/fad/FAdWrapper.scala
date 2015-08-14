package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sc.sjs.vm.util.domvm.{IndexedSuffixedDomId, FindElIndexedIdT}
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.DIV_WRAPPER_SUFFIX

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:15
 * Description: Враппер-div focused-карточки.
 */
object FAdWrapper extends FindElIndexedIdT with IndexedSuffixedDomId with FAdStatic {
  override protected def DOM_ID_SUFFIX = DIV_WRAPPER_SUFFIX
  override type T = FAdWrapper
}


trait FAdWrapperT extends SafeElT {
  override type T = HTMLDivElement
}


case class FAdWrapper(
  override val _underlying: HTMLDivElement
)
  extends FAdWrapperT
