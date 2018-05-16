package com.github.fisshy.react.scroll

import minitest._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.18 18:53
  * Description: Tests for react-scroll.
  */
object ScrollSpec extends SimpleTestSuite {

  test("render scrolled example vdom") {

    val vdom = <.div(

      <.div(
        Link(
          new LinkProps {
            override val to = "cont1"
          }
        )(
          "link1"
        ),

        Button(
          new LinkProps {
            override val to = "cont2"
          }
        )(
          "btn2"
        )
      ),


      <.div(
        Element(
          new ElementProps {
            override val name = "cont1"
          }
        )(
          <.div(
            "the content #1"
          )
        ),

        Element(
          new ElementProps {
            override val name = "cont2"
          }
        )(
          <.div(
            "THE CONTENT #2"
          )
        )
      )

    )

    val resStr = ReactDOMServer.renderToString( vdom )
    assert( resStr.nonEmpty )
  }

}
