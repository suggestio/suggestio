package com.mui.treasury.styles.switch

import minitest._

import scala.scalajs.js
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.07.2020 16:52
  */
object IosSpec extends SimpleTestSuite {

  test("makeStyles") {
    assert( js.typeOf(Ios) ==* "object" )
  }

}
