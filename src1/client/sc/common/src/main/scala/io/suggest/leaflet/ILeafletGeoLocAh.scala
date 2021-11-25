package io.suggest.leaflet

import diode.{ActionHandler, Effect}
import io.suggest.geo.MGeoLoc
import io.suggest.sc.model.dev.MScGeoLoc
import io.suggest.spa.DAction
import japgolly.univeq.UnivEq
import org.scalajs.dom

/** Interface for Leaflet+CdvBgGeo (or other non-HTML5) geolocation API implementations.
  * Marker trait for macwire DI compile-time dependency search.
  */
trait ILeafletGeoLocAh[M] extends ActionHandler[M, MScGeoLoc] {

  /** Effect for transferring location data into Leaflet's onLocation callback. */
  def leafletOnLocationFx( geoLoc: MGeoLoc ): Effect

  def isWatching(): Boolean

  def onLocationError(): Option[dom.PositionError => Unit]

}


/** Marker trait for actions, that must be routed into [[ILeafletGeoLocAh]] controller implementations. */
trait ILeafletGeoLocAction extends DAction


/** Marker trait for MGlSourceS.
  * Because MGlSourceS depends on Leaflet.js API, this trait abstracts out MGlSourceS internals.
  * LeafletGeoLocAh must hardly cast
  */
trait IGlSourceS
object IGlSourceS {
  @inline implicit def univEq: UnivEq[IGlSourceS] = UnivEq.force
}
