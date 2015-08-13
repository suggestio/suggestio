package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.Focused.CONTROLS_ID
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.08.15 18:32
 * Description: vm для контейнера вне-focused-body контента.
 */
object FControls extends FindDiv {
  override def DOM_ID = CONTROLS_ID
  override type T     = FControls
}


trait FControlsT extends SafeElT {
  override type T = HTMLDivElement
}


case class FControls(
  override val _underlying: HTMLDivElement
)
  extends FControlsT
