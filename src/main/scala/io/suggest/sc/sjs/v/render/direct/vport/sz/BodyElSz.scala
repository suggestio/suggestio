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

  def body: UndefOr[BodyEl] = js.native

}

sealed trait BodyEl extends js.Object {
  def clientWidth : UndefOr[Int] = js.native
  def clientHeight: UndefOr[Int] = js.native
}


/** Аддон для [[ViewportSzT]] для чтения размеров окна из document.body. */
trait BodyElSz extends IViewportSz {

  private def doc2safeDocBodyEl(doc: Document): BodyElSzEl = {
    doc.asInstanceOf[BodyElSzEl]
  }

  abstract override def getViewportSize: Option[ISize2di] = {
    super.getViewportSize orElse {
      val doc1 = doc2safeDocBodyEl(dom.document)
      for {
        bodyEl <- doc1.body.toOption
        w      <- bodyEl.clientWidth.toOption
        h      <- bodyEl.clientHeight.toOption
      } yield {
        Size2di(width = w, height = h)
      }
    }
  }

}
