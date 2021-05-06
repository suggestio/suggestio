package net.xdsoft.jodit.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.test._
import japgolly.scalajs.react.vdom.html_<^._
import minitest._

import scala.scalajs.js

object JoditEditorSpec extends SimpleTestSuite {

  test("JoditEditor.Js must be not null") {
    assert( JoditEditor.Js != null )
    assert( !js.isUndefined(JoditEditor.Js), s"JoditEditor.Js is ${JoditEditor.Js}")
  }


  test("JoditEditor component must be renderable") {
    val unmounted = JoditEditor.component(
      new JoditEditor.Props {
        override val value = "<p>Hello</p>"
      }
    )

    val resStr = ReactDOMServer.renderToString( unmounted )
    assert( resStr.nonEmpty )

    val outerComp = ScalaComponent.static {
      <.aside(
        unmounted
      )
    }

    ReactTestUtils.withRenderedIntoDocument( outerComp() ) { mounted =>
      val htmlStr = mounted.outerHtmlScrubbed()

      assert( htmlStr.nonEmpty, "Html tags not found." )
    }
  }

}
