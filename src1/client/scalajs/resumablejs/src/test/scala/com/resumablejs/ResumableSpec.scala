package com.resumablejs

import minitest._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.2020 15:30
  */
object ResumableSpec extends SimpleTestSuite {

  test("Instantiate: new Resumable(...)") {
    val inst = new Resumable(
      new ResumableOptions {}
    )
    assert( !js.isUndefined(inst.support) )
    assert( inst.version > 0 )
  }

}
