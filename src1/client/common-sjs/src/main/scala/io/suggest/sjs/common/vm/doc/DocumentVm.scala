package io.suggest.sjs.common.vm.doc

import io.suggest.sjs.common.vm.{IVm, Vm}
import io.suggest.sjs.common.vm.head.HeadVm
import org.scalajs.dom
import org.scalajs.dom.{Document, Element}
import org.scalajs.dom.raw.{HTMLBodyElement, HTMLDocument, HTMLElement, HTMLHeadElement}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 19:04
 * Description: Враппер для объекта document для возможности более безопасного (кросс-браузерного)
 * обращения к некоторым полям.
 */

/** Представление API document'а с точки зрения потенциальной опасности некоторых полей. */
@js.native
trait SafeDocumentApi extends js.Object {
  def body: UndefOr[HTMLBodyElement] = js.native
  def head: UndefOr[HTMLHeadElement] = js.native
  def scrollingElement: UndefOr[HTMLElement] = js.native
}
object SafeDocumentApi {
  implicit def apply(doc: Document): SafeDocumentApi = {
    doc.asInstanceOf[SafeDocumentApi]
  }
}


case class DocumentVm(override val _underlying: HTMLDocument = dom.document) extends IVm {

  override type T = Document

  protected def _safeGetTag[T <: HTMLElement](name: String)(dsf: SafeDocumentApi => UndefOr[T]): T = {
    val ds = SafeDocumentApi(_underlying)
    dsf(ds)
      .toOption
      .filter { _ != null }
      .getOrElse {
        _underlying
          .getElementsByTagName(name)(0)
          .asInstanceOf[T]
      }
  }

  /** Получить тег body, отрабатывая оптимальные и безопасные сценарии. */
  def body = _safeGetTag("body")(_.body)

  /** Получить тег head, отрабатывая оптимальные и безопасные сценарии. */
  def head = HeadVm( _safeGetTag("head")(_.head) )

  def scrollingElement: Element = {
    val ds = SafeDocumentApi( _underlying )
    ds.scrollingElement
      .getOrElse( _underlying.documentElement )
  }

  def documentElement = Vm( _underlying.documentElement )

}
