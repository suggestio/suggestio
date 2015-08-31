package io.suggest.sjs.common.view.safe.display

import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.15 17:41
 * Description: safe-поддержка свойства .outerHTML.
 * Firefox > 11
 * остальные браузеры обычно поддерживают всё без проблем.
 */

object OuterHtml extends SjsLogger {

  /** Константа-флаг, обозначающая умение (true) или беспомощьность(false) браузера делать outerHTML. */
  val HAS_OUTER_HTML: Boolean = {
    val el = dom.document
      .createElement("div")
    val stub = OuterHtmlStub( el )
    val hasOuter = !js.isUndefined( stub.outerHTML )
    if (!hasOuter)
      warn( WarnMsgs.NO_OUTER_HTML_SUPPORT )
    hasOuter
  }

}


/** Аддон safe-доступа к свойству outerHTML. */
trait OuterHtml extends ISafe {

  override type T <: Element

  /** Считывание всего html текущего элемента включая сам элемент.
    * Если браузер не поддерживает outerHTML, то будет эмуляция через innerHTML. */
  def outerHtml: String = {
    // Через anonymous div + .innerHTML.
    if (OuterHtml.HAS_OUTER_HTML) {
      _underlying.outerHTML
    } else {
      // Firefox < 11 или другой браузер, не умеющий читать outerHTML.
      val div = dom.document.createElement("div")
      div.appendChild(_underlying)
      div.innerHTML
    }
  }

}


/** undefined-доступ к свойству outerHTML. */
@js.native
sealed trait OuterHtmlStub extends js.Object {
  var outerHTML: UndefOr[String] = js.native
}
object OuterHtmlStub {
  def apply(e: Element): OuterHtmlStub = {
    e.asInstanceOf[OuterHtmlStub]
  }
}
