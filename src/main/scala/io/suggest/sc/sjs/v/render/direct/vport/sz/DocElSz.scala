package io.suggest.sc.sjs.v.render.direct.vport.sz

import io.suggest.adv.ext.model.im.{ISize2di, Size2di}
import org.scalajs.dom
import org.scalajs.dom.Document

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:10
 * Description: Безопасное чтение размеров viewport из documentElement.
 */
trait DocElSzEl extends js.Object {

  def documentElement: UndefOr[DocElWh] = js.native

}


sealed trait DocElWh extends js.Object {
  def clientWidth : UndefOr[Int] = js.native
  def clientHeight: UndefOr[Int] = js.native
}


/** Аддон для [[ViewportSzT]], добавляющий поддержку чтения размера окна из documentElement. */
trait DocElSz extends IViewportSz {

  private def doc2safeElSz(doc: Document): DocElSzEl = {
    doc.asInstanceOf[DocElSzEl]
  }

  abstract override def getViewportSize: Option[ISize2di] = {
    super.getViewportSize orElse {
      val doc1 = doc2safeElSz(dom.document)
      for {
        docEl   <- doc1.documentElement.toOption
        w       <- docEl.clientWidth.toOption
        h       <- docEl.clientHeight.toOption
      } yield {
        Size2di(width = w, height = h)
      }
    }
  }

}
