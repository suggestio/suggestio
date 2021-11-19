package io.suggest.geo

import boopickle.Default._

/** Boopickle stuff for geo-models. */
object GeoBooUtil {

  implicit val geoPointP: Pickler[MGeoPoint] = generatePickler

  implicit val circleGsP: Pickler[CircleGs] = generatePickler
  implicit val polygonGsP: Pickler[PolygonGs] = generatePickler
  implicit val pointGsP: Pickler[PointGs] = generatePickler
  implicit val lineStringP: Pickler[LineStringGs] = generatePickler
  implicit val multiPolygonGsP: Pickler[MultiPolygonGs] = generatePickler
  implicit val geoShapeP: Pickler[IGeoShape] = {
    // Сразу же защищаемся от рекурсивных пиклеров. Это особенно нужно для GeometryCollectionGs:
    implicit val geoShapeP = compositePickler[IGeoShape]

    geoShapeP
      .addConcreteType[CircleGs]              // Circle -- the main shape for end-users. Mandatory
      .addConcreteType[PolygonGs]             // Polygon. Shapes imported from OsmUtil or drawn via Umap. Mandatory.
      .addConcreteType[PointGs]
      .addConcreteType[LineStringGs]          // Polygon is just a several LineStrings. Optional, but near-zero overhead here.
      .addConcreteType[MultiPolygonGs]        // Just a several polygons. Not a big overhead.
      .addConcreteType[GeometryCollectionGs]  // May be useless here (recursive pickler for IGeoShape).
    // TODO Добавить поддержку остальных моделей при необходимости.
  }

  implicit val geoLocSourceP: Pickler[MGeoLocSource] = generatePickler
  implicit val geoLocP: Pickler[MGeoLoc] = generatePickler

}
