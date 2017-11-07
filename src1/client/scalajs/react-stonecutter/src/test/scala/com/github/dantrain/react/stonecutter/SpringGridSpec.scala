package com.github.dantrain.react.stonecutter

import minitest._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 21:48
  * Description: Tests for [[SpringGrid]].
  */
object SpringGridSpec extends SimpleTestSuite {

  test("js component must exist in namespace") {
    assert( !js.isUndefined(SpringGridJs) )
    assert(SpringGridJs != null)
  }

  test("Component must be usable") {
    val props = new SpringGridProps {
      override val columns = 4
      override val columnWidth = 20
    }
    val unmounted = SpringGrid(props)(
      <.div(
        "hello 1"
      ),
      <.div(
        "good bye 2"
      )
    )
    assert( !js.isUndefined(unmounted) )
    assert( unmounted != null )
  }

}
