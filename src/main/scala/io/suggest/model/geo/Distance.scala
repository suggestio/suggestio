package io.suggest.model.geo

import org.elasticsearch.common.unit.DistanceUnit
import DistanceUnit.{Distance => EsDistance}

object Distance {

  val parseDistance: PartialFunction[Any, Distance] = {
    case s: String =>
      val esDistance = EsDistance.parseDistance(s)
      Distance(esDistance)

    case esDistance: EsDistance =>
      Distance(esDistance)

    case dist: Distance =>
      dist
  }

  def apply(esDistance: EsDistance): Distance = {
    Distance(esDistance.value, esDistance.unit)
  }
}


/** Описание дистанции. */
case class Distance(distance: Double, units: DistanceUnit) {
  override def toString = units.toString(distance)

  def toEsDistance = new EsDistance(distance, units)
}


/** Описание того, как надо фильтровать по дистанции относительно какой-то точки на поверхности планеты. */
case class GeoDistanceQuery(center: GeoPoint, distanceMin: Option[Distance], distanceMax: Distance) {

  def outerCircle = CircleGs(center, distanceMax)

  def innerCircleOpt = distanceMin.map { CircleGs(center, _) }
}
