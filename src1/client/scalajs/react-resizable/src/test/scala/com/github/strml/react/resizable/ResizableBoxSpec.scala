package com.github.strml.react.resizable

import japgolly.scalajs.react.test._
import japgolly.scalajs.react.vdom.html_<^._
import minitest._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.18 11:54
  * Description: Tests for [[ResizableBox]].
  */
object ResizableBoxSpec extends SimpleTestSuite {

  test("js component must exist in namespace") {
    assert( !js.isUndefined(ResizableBoxJs) )
    assert( ResizableBoxJs != null )
  }

  test("Component must be usable") {
    val props = new ResizableBoxProps {
      override val height  = 100
      override val width   = 200
    }
    val unmounted = ResizableBox(props)(
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
