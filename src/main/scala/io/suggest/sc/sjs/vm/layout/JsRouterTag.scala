package io.suggest.sc.sjs.vm.layout

import io.suggest.sc.ScConstants.JsRouter.{DOM_ID => ID, URI}
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLScriptElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.08.15 12:06
 * Description: vm'ка для доступа к тегу js router'а.
 */
object JsRouterTag extends FindElT {

  override def DOM_ID = ID
  override type Dom_t = HTMLScriptElement
  override type T     = JsRouterTag

  def apply(): JsRouterTag = {
    val el = dom.document.createElement("script")
      .asInstanceOf[HTMLScriptElement]
    val res = JsRouterTag(el)
    res.initAttrs()
    res
  }

}


trait JsRouterTagT extends VmT {

  override type T = HTMLScriptElement

  /** Пересохранить все необходимые аттрибуты в script-тег. */
  def initAttrs(): Unit = {
    _underlying.src    = URI
    _underlying.`type` = "text/javascript"
    _underlying.async  = true
    _underlying.id     = JsRouterTag.DOM_ID
  }

  /** Добавить тег в конец body. */
  def appendToBody(): Unit = {
    DocumentVm()
      .body
      .appendChild(_underlying)
  }

}


case class JsRouterTag(
  override val _underlying: HTMLScriptElement
)
  extends JsRouterTagT
