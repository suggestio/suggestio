package io.suggest.sjs.common.view.safe.doc

import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom
import org.scalajs.dom.Document
import org.scalajs.dom.raw.{HTMLBodyElement, HTMLElement, HTMLHeadElement}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 19:04
 * Description: Враппер для объекта document для возможности более безопасного (кросс-браузерного)
 * обращения к некоторым полям.
 */
trait SafeDocumentT extends SafeEventTargetT {

  override type T = Document

  def _api(doc: Document = dom.document): SafeDocumentApi = {
    doc.asInstanceOf[SafeDocumentApi]
  }

  protected def _safeGetTag[T <: HTMLElement](name: String)(dsf: SafeDocumentApi => UndefOr[T]): T = {
    val d = _underlying
    val ds = _api(d)
    dsf(ds)
      .toOption
      .filter { _ != null }
      .getOrElse {
        d.getElementsByTagName(name)(0)
          .asInstanceOf[T]
      }
  }

  /** Получить тег body, отрабатывая оптимальные и безопасные сценарии. */
  def body = _safeGetTag("body")(_.body)

  /** Получить тег head, отрабатывая оптимальные и безопасные сценарии. */
  def head = _safeGetTag("head")(_.head)

}


/** Представление API document'а с точки зрения потенциальной опасности некоторых полей. */
trait SafeDocumentApi extends js.Object {
  def body: UndefOr[HTMLBodyElement] = js.native
  def head: UndefOr[HTMLHeadElement] = js.native
}


/** Дефолтовая реализация [[SafeDocumentT]], поля заменены на val и lazy val. */
case class SafeDocument(override val _underlying: Document = dom.document) extends SafeDocumentT {
  override lazy val head = super.head
  override lazy val body = super.body
}
