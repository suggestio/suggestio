package com.github.zpao.qrcode.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import minitest._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.2020 16:07
  * Description: Тесты для react-qrcode.
  */
object ReactQrCodeSpec extends SimpleTestSuite {

  test("js-component should be defined") {
    assert( !js.isUndefined(ReactQrCodeJs) )
    assert( ReactQrCodeJs != null )
  }

  test("sjs-component should be ok") {
    val vdom = <.div(
      ReactQrCode(
        new ReactQrCodeProps {
          override val value = "test test test"
        }
      )
    )

    val resStr = ReactDOMServer.renderToString( vdom )
    assert( resStr.nonEmpty )
  }

}
