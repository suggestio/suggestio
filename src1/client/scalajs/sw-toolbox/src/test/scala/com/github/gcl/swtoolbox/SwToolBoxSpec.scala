package com.github.gcl.swtoolbox

import minitest.SimpleTestSuite

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.18 11:50
  * Description: Tests for [[SwToolBox]].
  */
object SwToolBoxSpec extends SimpleTestSuite {

  test("initialize") {

    // TODO ReferenceError: self is not defined
    assert( !js.isUndefined( SwToolBox ) )

  }

}
