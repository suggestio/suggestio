package io.suggest.sjs.leaflet.path.circle

import io.suggest.sjs.leaflet.path.PathOptions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 22:29
  * Description: Circle options JS-object.
  */
trait CircleOptions extends PathOptions {

  /** Circle radius. */
  val radius  : Double

}
