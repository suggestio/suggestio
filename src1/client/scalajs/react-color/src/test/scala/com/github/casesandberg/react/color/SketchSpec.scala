package com.github.casesandberg.react.color

import minitest._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 10:50
  * Description: Tests for [[Sketch]] color picker.
  */
object SketchSpec extends SimpleTestSuite {

  test("Sketch is available in namespace") {
    val v = SketchJs
    assert( v != null && !js.isUndefined(v) )
  }

  test("Sketch jsComponent is not null") {
    assert( Sketch.component != null )
  }

  test("Sketch unmounted react component is not null") {
    val unmounted = Sketch(
      new SketchProps {
        override val color = "#fffaadd"
      }
    )
    assert( unmounted.toString.length > 0 )
    assert( unmounted != null && !js.isUndefined(unmounted) )
  }

}
