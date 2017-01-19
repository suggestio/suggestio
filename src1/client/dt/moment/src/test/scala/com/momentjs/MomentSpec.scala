package com.momentjs

import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 12:02
  * Description: Tests for moment.js [[Moment]] facade.
  */
object MomentSpec extends SimpleTestSuite {

  test("now is not null") {
    val m = Moment()
    assert( m != null )
  }

  test("now year is not less than 2017") {
    assert( Moment().year() >= 2017 )
  }

}
