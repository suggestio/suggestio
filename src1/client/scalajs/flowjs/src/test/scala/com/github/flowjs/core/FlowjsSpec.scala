package com.github.flowjs.core

import minitest._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.2020 15:30
  */
object FlowjsSpec extends SimpleTestSuite {

  test(s"Instantiate: new ${classOf[Flowjs].getClass.getSimpleName}(...)") {
    val inst = new Flowjs(
      new FlowjsOptions {}
    )
    assert( !js.isUndefined(inst.support) )
    assert( inst.version > 0 )
  }

}
