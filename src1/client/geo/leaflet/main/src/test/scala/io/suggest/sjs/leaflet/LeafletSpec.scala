package io.suggest.sjs.leaflet

import io.suggest.js.JsTypes
import minitest._
import japgolly.univeq._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.18 15:31
  * Description: Нерабочие тесты для Leaflet.
  * Он дёргает сразу window, которого нет.
  */
object LeafletSpec extends SimpleTestSuite {

  test("Leaflet must be imported") {
    assert( !js.isUndefined(Leaflet) )
    assert( Leaflet != null )
  }

  test("Leaflet.version must be a string") {
    assert( js.typeOf(Leaflet.version) ==* JsTypes.STRING )
    assert( Leaflet.version.nonEmpty )
  }

}
