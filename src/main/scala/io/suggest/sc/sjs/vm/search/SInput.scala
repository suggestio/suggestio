package io.suggest.sc.sjs.vm.search

import io.suggest.sc.sjs.m.msearch.{FtsFieldBlur, FtsFieldFocus, FtsFieldChanged}
import io.suggest.sc.sjs.vm.util.{InitOnEventToFsmUtilT, IInitLayout}
import io.suggest.sc.sjs.vm.util.domvm.FindElT
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLInputElement
import io.suggest.sc.ScConstants.Search.Fts.INPUT_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 14:29
 * Description: VM поля полнотекстового поиска.
 */
object SInput extends FindElT {

  override type Dom_t = HTMLInputElement
  override type T = SInput
  override def DOM_ID: String = INPUT_ID

}


/** Логика экземпляра модели поискового input'а в этом трейте. */
trait SInputT extends SafeElT with IInitLayout with InitOnEventToFsmUtilT {
  override type T = HTMLInputElement

  override def initLayout(): Unit = {
    _addToFsmEventListener("keyup", FtsFieldChanged)
    _addToFsmEventListener("focus", FtsFieldFocus)
    _addToFsmEventListener("blur",  FtsFieldBlur)
  }
}


/** Реализация модели поискового инпута. */
case class SInput(
  override val _underlying: HTMLInputElement
)
  extends SInputT
