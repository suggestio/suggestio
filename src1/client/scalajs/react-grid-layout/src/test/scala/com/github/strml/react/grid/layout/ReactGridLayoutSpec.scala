package com.github.strml.react.grid.layout

import minitest._

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 18:22
  * Description: Tests for RGL component sjs API.
  */
object ReactGridLayoutSpec extends SimpleTestSuite {

  test("Js component is defined") {
    val rgl = ReactGridLayoutJs
    assert( !js.isUndefined(rgl) )
    assert( rgl != null )
  }

  test("Instantiate js component") {
    val props = new ReactGridLayoutProps {
    }
    val rgl = ReactGridLayout(props)(
      <.div(
        ^.key := "1",
        ^.dataGrid := new Layout {
          override val x = 0
          override val y = 0
          override val w = 1
          override val h = 2
        },
        "asdasd"
      ),
      <.div(
        ^.key := "2",
        ^.dataGrid := new Layout {
          override val x = 2
          override val y = 2
          override val w = 3
          override val h = 4
        },
        "asdasdadsdasd"
      )
    )
    assert( rgl != null )
    assert( !js.isUndefined(rgl) )
  }

}
