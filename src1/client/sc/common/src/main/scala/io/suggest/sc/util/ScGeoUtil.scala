package io.suggest.sc.util

import io.suggest.geo.{IDistanceUtilJs, MGeoPoint}
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.sc.model.search.MGeoTabS
import io.suggest.scalaz.ScalazUtil.Implicits._
import scalaz.EphemeralStream

class ScGeoUtil(
                 distanceUtilJsOpt: => Option[IDistanceUtilJs]
               ) {

  /** Extract single geo.point from geo.shape, nearest to current map center. */
  def nodePropsShapesToNodeGeoPoint(nodePropsShapes: MGeoNodePropsShapes, v0: MGeoTabS): Option[MGeoPoint] = {
    for {
      distanceUtilJs <- distanceUtilJsOpt
      userLocPoint <- distanceUtilJs.prepareDistanceTo( v0.mapInit.state.center )
      near <- userLocPoint.nearestOf {
        (
          // At first, analyzing for center points:
          nodePropsShapes
            .shapes
            .iterator
            .flatMap(_.centerPoint) ##::
          // If no center points, use shape's any first point.
          nodePropsShapes
            .shapes
            .iterator
            .map(_.firstPoint) ##::
          // If no geo.shapes, use geoPoint associated with current node:
          nodePropsShapes.props.geoPoint.iterator ##::
          EphemeralStream[Iterator[MGeoPoint]]
        )
          .iterator
          .flatten
          .flatMap( distanceUtilJs.prepareDistanceTo )
          .nextOption()
      }
    } yield {
      near.geoPoint
    }
  }

}
