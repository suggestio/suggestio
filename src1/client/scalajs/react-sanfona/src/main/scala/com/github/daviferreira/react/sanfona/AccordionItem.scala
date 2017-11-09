package com.github.daviferreira.react.sanfona

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.11.17 17:55
  * Description: Scala.js API for AccordionItem component.
  */
object AccordionItem {

  val component = JsComponent[AccordionItemProps, Children.Varargs, js.Object]( AccordionItemJs )

  def apply(props: AccordionItemProps = new AccordionItemProps {})(children: VdomNode*) = component(props)(children: _*)

}


@js.native
@JSImport(REACT_SANFONA, "AccordionItem")
object AccordionItemJs extends js.Object


/** Properties JSON for instantiating each [[AccordionItem]]. */
trait AccordionItemProps extends js.Object {

  val title: js.UndefOr[String | js.Object] = js.undefined

  val expanded: js.UndefOr[Boolean] = js.undefined

  val onExpand, onClose: js.UndefOr[js.Function1[js.Object, _]] = js.undefined

  val className, bodyClassName, expandedClassName, titleClassName: js.UndefOr[String] = js.undefined

  val disabled: js.UndefOr[Boolean] = js.undefined

  val disabledClassName: js.UndefOr[String] = js.undefined

  val rootTag, titleTag, bodyTag: js.UndefOr[String] = js.undefined

  val duration: js.UndefOr[Int] = js.undefined

  val easing: js.UndefOr[String] = js.undefined

}
