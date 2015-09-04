package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sjs.common.vm.height3.SetHeight3Raw
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.vm.content.{ApplyFromOuterHtml, GetInnerHtml, OuterHtml}
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindElIndexedIdT
import io.suggest.sjs.common.vm.style.{StyleWidth, StyleLeft, StylePosition}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.15 16:26
 * Description: Ячейка в карусели focused-выдачи, является одновременно корневым div'ом каждой
 * focused-карточки, пришедшей от сервера.
 * Создаётся как контейнер, позиционируется, заполняется контентом, прицепляется к focused-карусели.
 */

object FAdRoot extends FindElIndexedIdT with FAdStatic with ApplyFromOuterHtml {

  override type T = FAdRoot

  /** Сборка focused root-vm из сырого HTML. */
  override def apply(outerHtml: String): FAdRoot = {
    val vm = super.apply(outerHtml)
    vm.positionAbsolute()
    vm
  }

}


trait FAdRootT extends VmT with StyleWidth with StyleLeft with StylePosition with _FAdFindSubtag with SetHeight3Raw
with GetInnerHtml with OuterHtml {

  override type T = HTMLDivElement

  // protected -> public
  override def setLeftPx(leftPx: Int) = super.setLeftPx(leftPx)

  def initLayout(screen: IMScreen, browser: IBrowser): Unit = {
    setWidthPx( screen.width )
    _setHeight3( screen.height, browser )
  }

  override type SubTagVm_t = FAdWrapper
  override protected type ContentVm_t = FAdContent
  override protected type SubTagEl_t = FAdWrapper.Dom_t
  override protected def _subtagCompanion = FAdWrapper
  override protected type SubtagCompanion_t = FAdWrapper.type

}


case class FAdRoot(
  override val _underlying: HTMLDivElement
)
  extends FAdRootT
{
  override lazy val wrapper = super.wrapper
}
