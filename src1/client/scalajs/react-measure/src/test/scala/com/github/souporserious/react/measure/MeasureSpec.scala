package com.github.souporserious.react.measure

import japgolly.scalajs.react.Ref
import minitest._

import scala.scalajs.js
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.08.2019 17:48
  * Description: Tests for Measure component.
  */
object MeasureSpec extends SimpleTestSuite {

  test("Native Measure component must exist in namespace") {
    assert( !js.isUndefined(MeasureJs) )
    assert(MeasureJs != null)
  }

  test("bounds() must work") {
    val unmounted = Measure.bounds { bounds: Bounds =>
      println("bounds => " + bounds)
    } { ref =>
      <.div.withRef( ref )(
        "test",
        <.br,
        "test"
      )
    }

    assert( !js.isUndefined(unmounted) )
    assert( unmounted != null )
  }


}
