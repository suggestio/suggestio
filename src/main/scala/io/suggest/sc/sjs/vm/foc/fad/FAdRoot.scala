package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.domvm.FindElIndexedIdT
import io.suggest.sc.sjs.vm.util.height3.SetHeight3Raw
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.css.{StyleLeft, Width}
import io.suggest.sjs.common.view.safe.display.{OuterHtml, GetInnerHtml, StylePosition}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.15 16:26
 * Description: Ячейка в карусели focused-выдачи, является одновременно корневым div'ом каждой
 * focused-карточки, пришедшей от сервера.
 * Создаётся как контейнер, позиционируется, заполняется контентом, прицепляется к focused-карусели.
 */

object FAdRoot extends FindElIndexedIdT with FAdStatic {
  override type T = FAdRoot

  /** Сборка focused root-vm из сырого HTML. */
  def apply(html: String): FAdRoot = {
    // Парсим через innerHTML вне DOM
    val div = VUtil.newDiv()
    div.innerHTML = html
    val rootDiv = div.firstChild.asInstanceOf[Dom_t]
    val res = apply(rootDiv)
    res.positionAbsolute()
    res
  }

}


trait FAdRootT extends SafeElT with Width with StyleLeft with StylePosition with _FAdFindSubtag with SetHeight3Raw
with GetInnerHtml with OuterHtml {

  override type T = HTMLDivElement

  // protected -> public
  override def setLeftPx(leftPx: Int) = super.setLeftPx(leftPx)

  def initLayout(screen: IMScreen): Unit = {
    setWidthPx( screen.width )
    _setHeight3( screen.height )
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
