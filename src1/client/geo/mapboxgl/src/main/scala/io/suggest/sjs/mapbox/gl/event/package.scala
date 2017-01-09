package io.suggest.sjs.mapbox.gl

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:31
  */
package object event {

  type Listener_t = js.Function1[EventData, _]

}
