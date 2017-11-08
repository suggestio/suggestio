package com.github.dantrain.react.stonecutter

import minitest._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 10:20
  * Description: Tests for [[makeResponsive]] exported HOC/function.
  */
object MakeResponsiveSpec extends SimpleTestSuite {

  test("makeResponsive must exist in namespace") {
    assert( !js.isUndefined(makeResponsive) )
    assert( makeResponsive != null )
  }


  test("makeResponsive/1 must be applicable to CSSGrid") {
    val cssGridComponent = CSSGrid.mkSjsComponent(
      makeResponsive( CSSGridJs )
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


  test("makeResponsive/2 must be applicable to SpringGrid") {
    val hocOpts = new MakeResponsiveOptions {
      override val maxWidth = 1920
      override val minPadding = 2
      override val defaultColumns = 6
    }
    val springGridComponent = SpringGrid.mkSjsComponent(
      makeResponsive( SpringGridJs, hocOpts )
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
