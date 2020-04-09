package io.suggest.sjs

import org.scalajs.dom.raw

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.12.18 13:00
  * Description: Единый импорт для поддержки улучшенных dom-интерфейсов.
  */
package object dom2 {

  @inline implicit def posOptsToRaw(posOpts: PositionOptions): raw.PositionOptions =
    posOpts.asInstanceOf[raw.PositionOptions]

  @inline implicit def geolocationFromRaw(geolocation: raw.Geolocation): Geolocation =
    geolocation.asInstanceOf[Geolocation]

  /** Тип идентификатора watch'ера в Geolocation API. */
  type GeoLocWatchId_t = js.Any

}
