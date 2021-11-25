package io.suggest.leaflet

/** Переброска в GeoLocAh запроса из Leaflet.Map().locate() и stopLocation(). */
case class GlLeafletApiLocate(locateOpts: Option[GlLeafletLocateArgs] ) extends ILeafletGeoLocAction

/** Таймаут запроса геолокации из leaflet. */
case object GlLeafletApiLocateTimeout extends ILeafletGeoLocAction
