package com.github.daviferreira.react.sanfona

import minitest._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.11.17 18:17
  * Description:
  */
object AccordionSpec extends SimpleTestSuite {

  test("Accordion component must be in namespace") {
    assert( !js.isUndefined(AccordionJs) )
    assert( AccordionJs != null )
  }

  test("AccordionItem also must exist in namespace") {
    assert( !js.isUndefined(AccordionItemJs) )
    assert( AccordionItemJs != null )
  }

  test("Accordion must be usable as JsComponent") {
    val props = new AccordionProps {
      override val allowMultiple = false
    }

    val accordion = Accordion(props)(
      AccordionItem.component.withKey("a")(
        new AccordionItemProps {
          override val disabled = false
        }
      )(
        <.div(
          "test A"
        )
      ),

      AccordionItem.component.withKey("b")(
        new AccordionItemProps {
          override val disabled = true
        }
      )(
        <.div(
          "test B"
        )
      )
    )

    assert( accordion.toString.length > 0 )
  }

}
