package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sc.sjs.vm.util.domvm.get.{ISubTagElT, ISubTagCompanion, IFindSubTag}
import io.suggest.sc.sjs.vm.util.domvm.{DomId, IApplyEl}
import io.suggest.sc.ScConstants.Focused.FAd.ID_PREFIX
import io.suggest.sjs.common.view.safe.ISafe
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


/** Костылище для использования цепочки root-wrapper-content вместе с трейтом SetHeight3 и ChildElOrFind.
  * Этот трейт должен быть mixin'ом, чтобы решать проблемы с abstact override _findSubtag() в одном из связанных трейтов. */
trait _FAdFindSubtag extends IFindSubTag with ISafe with ISubTagCompanion with ISubTagElT {
  override type T = HTMLDivElement
  override protected type SubTagEl_t = HTMLDivElement
  override protected def _findSubtag(): Option[SubTagVm_t] = {
    Option( _underlying.firstChild.asInstanceOf[SubTagEl_t] )
      .map { _subtagCompanion.apply }
  }

  override protected type SubtagCompanion_t <: FAdStatic { type T = SubTagVm_t }
}
