package io.suggest.sc.sjs.vm.res

import io.suggest.sc.ScConstants.Rsc._
import io.suggest.sc.sjs.vm.layout.LayContentVm
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 16:09
 * Description: Контейнер для focused-ресурсов.
 */
object FocusedRes extends ResStaticT {

  override def DOM_ID = FOCUSED_ID
  override type T = FocusedRes

  override protected def _insertDiv(lay: LayContentVm, div: HTMLDivElement): Unit = {
    lay.insertAfterFirst(div)
  }
}


case class FocusedRes(
  override val _underlying: HTMLDivElement
) extends ResT
