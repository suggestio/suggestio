package com.github.mikbry.materialui.color

import japgolly.scalajs.react.ReactDOMServer
import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 11:14
  * Description: Тесты для ColorPicker API.
  */
object ColorPickerSpec extends SimpleTestSuite {

  test("ColorPicker API must be usable") {
    val unmounted = ColorPicker.component(
      new ColorPicker.Props {
        override val value = "#fff"
      }
    )

    val resStr = ReactDOMServer.renderToString( unmounted )
    assert( resStr contains "MuiButton" )

    // TODO Почему-то не работает тестовый рендер. Exception где-то внутри ReactTestUtils, сам компонент вроде исправно работает внутри react.js.
    /*ReactTestUtils.withRenderedIntoDocument( unmounted ) { mounted =>
      val htmlStr = mounted.outerHtmlScrubbed()

      assert( htmlStr.nonEmpty, "Html tags not found." )
    }*/
  }

}
