package com.github.souporserious.react.measure

import minitest._
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react.raw.PropsChildren

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
    val unmounted = Measure {
      new MeasureProps {
        override val children: js.Function1[MeasureChildrenArgs, PropsChildren] = { chArgs =>
          <.div(
            ^.genericRef := chArgs.measureRef,
            "test",
            <.br,
            "test"
          )
            .rawElement
        }
      }
    }

    assert( !js.isUndefined(unmounted) )
    assert( unmounted != null )
  }

}
