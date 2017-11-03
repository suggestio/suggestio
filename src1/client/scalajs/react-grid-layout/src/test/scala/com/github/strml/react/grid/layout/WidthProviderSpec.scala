package com.github.strml.react.grid.layout

import io.suggest.js.JsTypes
import japgolly.scalajs.react.vdom.html_<^._
import minitest._

import scala.scalajs.js
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 19:55
  * Description: Tests for [[WidthProvider]].
  */
object WidthProviderSpec extends SimpleTestSuite {

  test("WidthProvider defined") {
    val wp = WidthProviderJs
    assert( !js.isUndefined(wp) && wp != null )
  }

  test("WidthProvider is a HOC (js-function)") {
    val wp = WidthProviderJs
    val wpJsType = js.typeOf(wp)
    assert( wpJsType ==* JsTypes.FUNCTION, wpJsType )
  }


  test("WidthProvider successfully wraps Responsive") {
    val comp = WidthProvider( ResponsiveJs )
    val rglWp = comp(
      new ResponsiveProps with WidthProviderProps {

        override val measureBeforeMount: Boolean = true

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
    )(
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

    assert( !js.isUndefined(rglWp) && rglWp != null )

  }

}
