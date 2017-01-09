package io.suggest.sjs.common.vsz

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:10
 * Description: Безопасное чтение размеров viewport из documentElement.
 */
@js.native
trait DocElSzEl extends js.Object {

  def documentElement: UndefOr[ClientElementWhSafe] = js.native

}


/** Аддон для [[ViewportSz]], добавляющий поддержку чтения размера окна из documentElement. */
trait DocElSz extends IViewportSz {

  private object DocElSzHelper extends ElSafeGetIntValueT {
    override def _elSafe: UndefOr[ClientElementWhSafe] = {
      dom.document
        .asInstanceOf[DocElSzEl]
        .documentElement
    }
  }

  import DocElSzHelper.getValue

  abstract override def widthPx: Option[Int] = {
    getValue(super.widthPx)(_.clientWidth)
  }

  abstract override def heightPx: Option[Int] = {
    getValue(super.heightPx)(_.clientHeight)
  }

}
