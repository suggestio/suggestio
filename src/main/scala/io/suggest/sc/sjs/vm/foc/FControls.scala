package io.suggest.sc.sjs.vm.foc

import io.suggest.primo.IReset
import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sc.sjs.m.mfoc.{ProducerLogoClick, CloseBtnClick}
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.{OnClick, IInitLayout, ClearT}
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.Focused.{CONTROLS_ID, CLOSE_BTN_CLASS}
import io.suggest.sc.ScConstants.Logo.HDR_LOGO_DIV_CLASS
import io.suggest.sjs.common.view.safe.{SafeEl, SafeElT}
import io.suggest.sjs.common.view.safe.display.{GetInnerHtml, SetInnerHtml}
import org.scalajs.dom.{Node, Event}
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


/** Логика focused controls вынесена сюда. */
trait FControlsT extends SafeElT with SetInnerHtml with ClearT with IInitLayout with OnClick with GetInnerHtml
with IReset {

  override type T = HTMLDivElement

  override def initLayout(): Unit = {
    // Повесить события. Т.к. header динамический, сюда делегируются события всех подэлементов.
    onClick { e: Event =>
      // Безопасно ли здесь к Node приводить тип? svg-элементы вроде безопасны в этом плане.
      val safeTarget = SafeEl( e.target.asInstanceOf[Node] )
      val f = VUtil.hasCssClass(safeTarget, _: String)
      val msgOpt = f(CLOSE_BTN_CLASS)
        .map { closeBtnDiv => CloseBtnClick }
        .orElse {
          f(HDR_LOGO_DIV_CLASS)
            .map { logoDiv => ProducerLogoClick }
        }
        // TODO Клик по кнопке перехода на плитку продьюсера.
      for (msg <- msgOpt) {
        ScFsm !! msg
      }
    }
  }

  override def reset(): Unit = {
    clear()
  }

}


case class FControls(
  override val _underlying: HTMLDivElement
)
  extends FControlsT
