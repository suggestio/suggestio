package com.github.dominictobias.react.image.crop

import minitest._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 15:47
  * Description: Tests for [[ReactCrop]].
  */
object ReactCropSpec extends SimpleTestSuite {

  test("js-component should be defined") {
    assert( !js.isUndefined(ReactCropJs) )
    assert( ReactCropJs.toString.length > 0 )
  }

  test("js-component should wrap in sjs") {
    val props = new ReactCropProps {
      override val src = "https://x.y/z.jpg"
    }
    val comp = ReactCrop(props)
    assert( !js.isUndefined(comp) )
    assert( comp.toString.length > 0 )
  }

}
