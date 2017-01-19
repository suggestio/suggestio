package io.suggest.dt.moment

import com.momentjs.Moment
import io.suggest.dt.MYmd
import minitest._

import scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 12:09
  * Description: Tests for [[MomentJsUtil]].
  */
object MomentJsUtilSpec extends SimpleTestSuite {

  import MomentJsUtil.Implicits.MomentDateExt

  test("DateExt should Moment -> MYmd") {
    val m = Moment( js.Array(2017,0,3) )
    val ymd = MYmd.from(m)
    assertEquals(ymd, MYmd(2017,1,3))
  }

  test("DateExt should MYmd -> Moment") {
    val ymd = MYmd(2017,1,3)
    val m = ymd.to[Moment]
    assertEquals( m.year(),   2017)
    assertEquals( m.month(), 0)
    assertEquals( m.date(),   3)
  }

}
