package com.github.strml.react.resizable

import minitest._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.test._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.18 11:25
  * Description: Tests for [[Resizable]] js-component.
  */
object ResizableSpec extends SimpleTestSuite {

  test("js component must exist in namespace") {
    assert( !js.isUndefined(ResizableJs) )
    assert( ResizableJs != null )
  }

  test("Component must be usable") {
    val props = new ResizableProps {
      override val height  = 100
      override val width   = 200
    }
    val unmounted = Resizable(props)(
      <.div(
        "Content"
      )
    )
    ReactTestUtils.withRenderedIntoDocument( unmounted ) { mounted =>
      val htmlStr = mounted.outerHtmlScrubbed()
      assert( htmlStr.length > 0 )
    }
  }

}
