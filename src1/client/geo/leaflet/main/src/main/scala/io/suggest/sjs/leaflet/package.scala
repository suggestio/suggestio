package io.suggest.sjs

import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js.`|`

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:45
  */
package object leaflet {

  def L = Leaflet

  type MapTarget = String | HTMLElement

}
