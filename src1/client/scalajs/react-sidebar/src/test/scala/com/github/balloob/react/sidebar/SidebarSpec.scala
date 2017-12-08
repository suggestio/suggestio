package com.github.balloob.react.sidebar

import minitest._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.17 12:20
  * Description: Tests for [[Sidebar]] binding.
  */
object SidebarSpec extends SimpleTestSuite {

  test("js-component should exist") {
    assert( SidebarJs != null )
    assert( !js.isUndefined(SidebarJs) )
  }


  test("sjs-component should be usable") {
    val sidebarContent = <.div(
      "item 1",
      "item 2",
      "item 3"
    )

    val comp = Sidebar {
      new SidebarProps {
        override val sidebar    = sidebarContent.rawNode
        override val docked     = false
        override val open       = false
        override val pullRight  = false
        override val shadow     = true
      }
    }(
      <.div(
        <.strong(
          "some outer content",
        ),
        <.div(
          "with some other content"
        )
      ),

      <.div(
        "some child #2"
      )
    )

    assert( !js.isUndefined(comp) )
    assert( comp.toString.length > 0 )
  }

}
