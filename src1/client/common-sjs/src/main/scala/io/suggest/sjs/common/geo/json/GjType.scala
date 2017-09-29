package io.suggest.sjs.common.geo.json

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 16:24
  * Description: Интерфейс типа GeoJSON объекта, который есть у всех объектов там.
  */

trait GjType extends js.Object {

  val `type`: String

}
