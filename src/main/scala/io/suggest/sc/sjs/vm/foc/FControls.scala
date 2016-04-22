package io.suggest.sc.sjs.vm.foc

import io.suggest.primo.IReset
import io.suggest.sc.sjs.m.mfoc.{CloseBtnClick, ProducerLogoClick}
import io.suggest.sc.sjs.vm.util.OnClick
import io.suggest.sc.ScConstants.Focused.{CLOSE_BTN_CLASS, CONTROLS_ID}
import io.suggest.sc.ScConstants.Logo.HDR_LOGO_DIV_CLASS
import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.content.{ClearT, GetInnerHtml, SetInnerHtml}
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.util.IInitLayout
import io.suggest.sjs.common.vm.{Vm, VmT}
import org.scalajs.dom.{Event, Node}
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
trait FControlsT extends VmT with SetInnerHtml with ClearT with IInitLayout with OnClick with GetInnerHtml
with IReset {

  override type T = HTMLDivElement

  override def initLayout(): Unit = {
    // Повесить события. Т.к. header динамический, сюда делегируются события всех подэлементов.
    onClick { e: Event =>
      // Безопасно ли здесь к Node приводить тип? svg-элементы вроде безопасны в этом плане.
      val safeTarget = Vm( e.target.asInstanceOf[Node] )
      val f = VUtil.hasCssClass(safeTarget, _: String)
      f(CLOSE_BTN_CLASS)
        .map { closeBtnDiv => CloseBtnClick }
        .orElse {
          f(HDR_LOGO_DIV_CLASS)
            .map { logoDiv => ProducerLogoClick }
        }
        // Отправить сигнал о событии в FSM, если таковой имеется.
        .foreach { msg =>
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
