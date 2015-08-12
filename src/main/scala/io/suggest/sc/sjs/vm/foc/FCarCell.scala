package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.css.{StyleLeft, Width}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.15 16:26
 * Description: Ячейка в карусели focused-выдачи.
 * Создаётся как контейнер, позиционируется, заполняется контентом, прицепляется к карусели [[FCarousel]].
 */

object FCarCell {

  def apply(): FCarCell = {
    val div = VUtil.newDiv()
    div.style.position = "relative"
    FCarCell(div)
  }

}


trait FCarCellT extends SafeElT with Width with StyleLeft {

  override type T = HTMLDivElement

  def setContent(html: String): Unit = {
    _underlying.innerHTML = html
  }

  // protected -> public
  override def setWidthPx(widthPx: Int) = super.setWidthPx(widthPx)
  override def setLeftPx(leftPx: Int) = super.setLeftPx(leftPx)

}


case class FCarCell(
  override val _underlying: HTMLDivElement
)
  extends FCarCellT
