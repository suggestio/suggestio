package com.github.dantrain.react.stonecutter

import minitest._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 10:32
  * Description: Tests for [[measureItems]] function/HOC.
  */
object MeasureItemsSpec extends SimpleTestSuite {

  test("measureItems must exist in namespace") {
    assert( !js.isUndefined(measureItems) )
    assert( measureItems != null )
  }


  test("measureItems/1 must be applicable to CSSGrid") {
    val cssGridComponent = CSSGrid.mkSjsComponent(
      measureItems( CSSGridJs )
    )

    val compProps = new CssGridProps {
      override val duration       = 1000
      override val columns        = 4
      override val columnWidth    = 30
      override val component      = GridComponents.DIV
    }
    val unmounted = cssGridComponent(compProps)(
      <.div(
        "one"
      ),
      <.div(
        "two"
      ),
      <.div(
        "three"
      )
    )

    assert( !js.isUndefined(unmounted) )
    assert( unmounted != null )
  }


  test("measureItems/2 must be applicable to SpringGrid") {
    val hocOpts = new MeasureItemsOptions {
      override val measureImages = false
    }
    val springGridComponent = SpringGrid.mkSjsComponent(
      measureItems( SpringGridJs, hocOpts )
    )

    val compProps = new SpringGridProps {
      override val columns      = 4
      override val columnWidth  = 25
      override val component    = GridComponents.DIV
    }
    val unmounted = springGridComponent(compProps)(
      <.div(
        "one"
      ),
      <.div(
        "two"
      ),
      <.div(
        "three"
      )
    )

    assert( !js.isUndefined(unmounted) )
    assert( unmounted != null )

    // TODO
    //val renderDiv = dom.document.createElement("div")
    //unmounted.renderIntoDOM( renderDiv )
  }

}
