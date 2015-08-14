package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sc.sjs.vm.util.domvm.{DomId, IApplyEl}
import io.suggest.sc.ScConstants.Focused.FAd.ID_PREFIX
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:15
 * Description: Утиль для сборки похожих FAd* vm'ок.
 */

/** Трейт для статических моделей FAd*. */
trait FAdStatic extends IApplyEl with DomId {
  override type Dom_t = HTMLDivElement
  override def DOM_ID = ID_PREFIX
}
