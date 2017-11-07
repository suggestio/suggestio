package com.github.dantrain.react.stonecutter

import minitest._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 21:53
  * Description: Tests for [[CSSGrid]] component.
  */
object CSSGridSpec extends SimpleTestSuite {

  test("js component must exist in namespace") {
    assert( !js.isUndefined(CSSGridJs) )
    assert( CSSGridJs != null )
  }

  test("Component must be usable") {
    val props = new CssGridProps {
      override val duration    = 1000
      override val columns     = 4
      override val columnWidth = 20
      override val component   = GridComponents.DIV
    }
    val unmounted = CSSGrid(props)(
      <.div(
        "asdasd"
      ),
      <.div(
        "gegegrg"
      )
    )
    assert( !js.isUndefined(unmounted) )
    assert( unmounted != null )
  }

}
