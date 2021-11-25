package io.suggest.geo

/** Some distance-calculating functions dirty-implemented via 3rd-party libraries,
  * that may be not available on target configuration: for example Leaflet not compatible with showcase-SSR,
  * but distance measurments in showcase implemented via Leaflet's LatLng.
  *
  * To isolate showcase distance implementation from Leaflet, this class is used.
  *
  * TODO Possibly, this must be in [maps-util] subproject.
  */
trait IDistanceUtilJs {

  type T

  /** Prepare one's point data be distance-measured.
    * @return None, if not implemented/not available.
    */
  def prepareDistanceTo(geoPoint: MGeoPoint): Option[IDistanceFrom[T]]

  /** Calculate center of polygon (center point of bounding rect.). */
  def geoCenterOfPolygon(poly: ILPolygonGs): Option[IDistanceFrom[T]]

}


/** Optimized for distance measurments implementation of geo-point. */
trait IDistanceFrom[T] {

  /** Original geo-point. */
  def geoPoint: MGeoPoint

  /** Implementation-specific geo point representation, optimized for distance measuremets. */
  def geoPointImpl: T

  /** Get geo-distance to other point in meters
    * @return Distance in meters.
    */
  def distanceTo(to: IDistanceFrom[T]): Double

  def nearestOf( points: IterableOnce[IDistanceFrom[T]] ): Option[IDistanceFrom[T]] = {
    val baseIter = points.iterator
    val buffIter = baseIter.buffered
    for (first <- buffIter.headOption) yield {
      if (baseIter.isEmpty) {
        first
      } else {
        buffIter
          .minBy { currPoint =>
            distanceTo( currPoint )
          }
      }
    }
  }

}
