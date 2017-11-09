package com.github.daviferreira.react.sanfona

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.11.17 17:55
  * Description: Scala.js API facade for Accordion component.
  */
object Accordion {

  val component = JsComponent[AccordionProps, Children.Varargs, AccordionState]( AccordionJs )

  def apply(props: AccordionProps = new AccordionProps {})(children: VdomNode*) = component( props )(children: _*)

}


@JSImport(REACT_SANFONA, "Accordion")
@js.native
object AccordionJs extends js.Object


/** Accordion internal state object interface. */
@js.native
trait AccordionState extends js.Object


/** Properties for instantiation [[Accordion]]/[[AccordionJs]]. */
trait AccordionProps extends js.Object {

  val allowMultiple: js.UndefOr[Boolean] = js.undefined

  val openNextAccordionItem: js.UndefOr[Boolean] = js.undefined

  val className: js.UndefOr[String] = js.undefined

  val style: js.UndefOr[js.Object] = js.undefined

  val onChange: js.UndefOr[js.Function1[AccordionState, _]] = js.undefined

  val rootTag: js.UndefOr[String] = js.undefined

  val duration: js.UndefOr[Int] = js.undefined

  val easing: js.UndefOr[String] = js.undefined

}
