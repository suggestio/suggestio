package io.suggest.geo

import io.suggest.primo.IApply1

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.17 15:28
  * Description: Система моделей гео-шейпов, не привязанных ни к каким конкретным реализациям.
  * Изначально сформировалась на сервере в [util] для отражения географии в ES.
  * Потом переехала в [mgeo], затем классы моделей стали совсем кросс-платфроменными и переехали сюда.
  */

/** Базовый трейт для реализаций geoshape. */
sealed trait IGeoShape {

  /** Используемый тип фигуры. */
  def shapeType: GsType

  def firstPoint: MGeoPoint

  /**
    * Центральная точка фигуры.
    * По идее, эта точка всегда существует, но тут Option.
    * None означает, что код поддержки вычисления центральной точки не заимплеменчен.
    */
  def centerPoint: Option[MGeoPoint] = None

  /** Отображаемое для пользователя имя шейпа. */
  def displayTypeName: String = {
    shapeType.geoJsonName
      .getOrElse( getClass.getSimpleName )
  }

}


/** Если элемент можно запрашивать в geo-shape search/filter, то об этом можно уведомить компилятор. */
sealed trait IGeoShapeQuerable extends IGeoShape


// Реализация GeoShape'ов:

/** Гео-шейп точки. */
case class PointGs(coord: MGeoPoint) extends IGeoShapeQuerable {
  override def shapeType = GsTypes.Point
  override def firstPoint = coord
  override def centerPoint = Some(coord)
}


/** Гео-шейп круга. */
// TODO Замёржить common-модель MGeoCircle в этот шейп.
case class CircleGs(
                     center   : MGeoPoint,
                     radiusM  : Double
                   )
  extends IGeoShapeQuerable
{
  override def shapeType = GsTypes.Circle
  override def firstPoint = center
  override def centerPoint = Some(center)
}


/** Гео-шейп квадрата. */
case class EnvelopeGs(
                       topLeft     : MGeoPoint,
                       bottomRight : MGeoPoint
                     )
  extends IGeoShapeQuerable
{

  override def shapeType = GsTypes.Envelope
  override def firstPoint: MGeoPoint = topLeft

  override def centerPoint: Some[MGeoPoint] = {
    // TODO Код не тестирован и не использовался с момента запиливания
    // Тут чисто-арифметическое определение центра, без [возможных] поправок на форму геойда и прочее.
    val c = MGeoPoint(
      lat = (bottomRight.lat + topLeft.lat) / 2,
      lon = (bottomRight.lon + topLeft.lon) / 2
    )
    Some(c)
  }

}


/** Гео-шейп коллекций любых других геометрий. Не является Querable. */
case class GeometryCollectionGs(geoms: Seq[IGeoShape]) extends IGeoShape {
  override def shapeType = GsTypes.GeometryCollection
  override def firstPoint = geoms.head.firstPoint
}


/** Общий интерфейс для [[LineStringGs]] и [[MultiPointGs]] обитает здесь. */
sealed trait MultiPointShape extends IGeoShapeQuerable {
  def coords: Seq[MGeoPoint]
  override def firstPoint = coords.head
}

/** Гео-шейп для MultiPoint geometry. */
case class MultiPointGs(coords: Seq[MGeoPoint]) extends MultiPointShape {
  override def shapeType = GsTypes.MultiPoint
}
object MultiPointGs extends IApply1 {
  override type ApplyArg_t = Seq[MGeoPoint]
  override type T = MultiPointGs
}

/** Гео-шейп для ListString geometry. */
case class LineStringGs(coords: Seq[MGeoPoint]) extends MultiPointShape {
  override def shapeType = GsTypes.LineString
  override def firstPoint = coords.head
}
object LineStringGs extends IApply1 {
  override type ApplyArg_t = Seq[MGeoPoint]
  override type T = LineStringGs
}


/** Гео-шейп мульти-линии. */
case class MultiLineStringGs(lines: Seq[LineStringGs]) extends IGeoShapeQuerable {
  override def shapeType = GsTypes.MultiLineString
  override def firstPoint = lines.head.firstPoint
}


/** Гео-шейп полигона с необязательными дырками в двумерном пространстве. */
case class PolygonGs(
                      outer : LineStringGs,
                      holes : List[LineStringGs] = Nil
                    )
  extends IGeoShapeQuerable
{
  override def shapeType = GsTypes.Polygon
  override def firstPoint = outer.firstPoint
  def toMpGss = outerWithHoles.map(_.coords)
  def outerWithHoles = outer :: holes
}


/** Гео-шейп мульти-полигона. */
case class MultiPolygonGs(polygons: Seq[PolygonGs]) extends IGeoShapeQuerable {
  override def shapeType = GsTypes.MultiPolygon
  override def firstPoint = polygons.head.firstPoint
}
