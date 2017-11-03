package com.github.strml.react.grid.layout

import japgolly.scalajs.react.vdom.html_<^._
import minitest._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 18:22
  * Description: Tests for RGL component sjs API.
  */
object ResponsiveSpec extends SimpleTestSuite {

  test("Js component is defined") {
    val rgl = ResponsiveJs
    assert( !js.isUndefined(rgl) )
    assert( rgl != null )
  }

  test("Instantiate js component using example from README") {
    val props = new ResponsiveProps {
      override val width = 760

      override val layouts = js.defined {
        val lgLayouts = js.Array[Layout](
          new Layout {
            override val i = "1"
            override val x = 0
            override val y = 0
            override val w = 1
            override val h = 2
          },
          new Layout {
            override val i = "2"
            override val x = 2
            override val y = 2
            override val w = 3
            override val h = 4
          }
        )
        js.Dictionary(
          "lg" -> lgLayouts
        )
      }

      override val breakpoints = js.defined {
        js.Dictionary[Int](
          "lg"  -> 1200,
          "md"  -> 996,
          "sm"  -> 768,
          "xs"  -> 480,
          "xxs" -> 260
        )
      }

      override val cols = js.defined {
        js.Dictionary[Int](
          "lg"  -> 12,
          "md"  -> 10,
          "sm"  -> 6,
          "xs"  -> 4,
          "xxs" -> 2
        )
      }

    }

    val rgl = Responsive(props)(
      <.div(
        ^.key := "1",
        "1"
      ),
      <.div(
        ^.key := "2",
        "2"
      ),
      <.div(
        ^.key := "3",
        "3"
      )
    )

    assert( rgl != null )
    assert( !js.isUndefined(rgl) )
  }

}
