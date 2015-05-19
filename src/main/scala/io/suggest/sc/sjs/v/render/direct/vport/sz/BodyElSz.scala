package io.suggest.sc.sjs.v.render.direct.vport.sz

import io.suggest.adv.ext.model.im.{ISize2di, Size2di}
import org.scalajs.dom
import org.scalajs.dom.Document

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:23
 * Description: Определение размера viewport через document.body.
 */
trait BodyElSzEl extends js.Object {

  def body: UndefOr[ClientElementWhSafe] = js.native

}


/** Аддон для [[ViewportSzT]] для чтения размеров окна из document.body. */
trait BodyElSz extends IViewportSz {

  private object BodyElSzHelper extends ElSafeGetIntValueT {
    override def _elSafe: UndefOr[ClientElementWhSafe] = {
      dom.document
        .asInstanceOf[BodyElSzEl]
        .body
    }
  }

  import BodyElSzHelper.getValue

  abstract override def widthPx: Option[Int] = {
    getValue(super.widthPx)(_.clientWidth)
  }

  abstract override def heightPx: Option[Int] = {
    getValue(super.heightPx)(_.clientHeight)
  }

}
