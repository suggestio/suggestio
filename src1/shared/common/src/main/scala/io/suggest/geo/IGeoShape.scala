package io.suggest.geo

import boopickle.Default._
import io.suggest.common.maps.rad.IMinMaxM
import io.suggest.primo.IApply1

import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.17 15:28
  * Description: Система моделей гео-шейпов, не привязанных ни к каким конкретным реализациям.
  * Изначально сформировалась на сервере в [util] для отражения географии в ES.
  * Потом переехала в [mgeo], затем классы моделей стали совсем кросс-платфроменными и переехали сюда.
  */
object IGeoShape {

  /** Общий boopickler для ИСПОЛЬЗУЕМЫХ реализаций [[IGeoShape]]. */
  implicit val GEO_SHAPE_PICKLER: Pickler[IGeoShape] = {
    // Добавлены только необходимые элементы.
    implicit val circleGsP = CircleGs.CIRCLE_GS_PICKLER
    implicit val polygonGsP = PolygonGs.POLYGON_GS_PICKLER
    implicit val pointGsP = PointGs.POINT_GS_PICKLER
    implicit val lineStringGsP = LineStringGs.LINE_STRING_PICKLER
    implicit val multiPolygonGsP = MultiPolygonGs.MULTI_POLYGON_GS_PICKLER

    // Сразу же защищаемся от рекурсивных пиклеров. Это особенно нужно для GeometryCollectionGs:
    implicit val geoShapeP = compositePickler[IGeoShape]

    geoShapeP
      .addConcreteType[CircleGs]              // ГеоКруг -- основная гео-фигура в личном кабинете.
      .addConcreteType[PolygonGs]             // OsmUtil. Карты в ЛК уже вовсю рендерят полигоны.
      .addConcreteType[PointGs]
      .addConcreteType[LineStringGs]          // OsmUtil. PolygonGs тянет её за собой всё равно.
      .addConcreteType[MultiPolygonGs]        // OsmUtil
      .addConcreteType[GeometryCollectionGs]  // OsmUtil, но в реале оно наверное не нужно.
      // TODO Добавить поддержку остальных моделей при необходимости.
  }

}

/** Базовый трейт для всех моделей гео-шейпов. */
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

  /** Занимает ли данная фигура область на карте?
    * @return true Фигура занимает какую-то область на карте.
    *         false Фигура является точкой или линией, она не занимает никаких областей на карте.
    */
  def isArea: Boolean = {
    shapeType.isArea.contains(true)
  }

  /** Является ли шейп leaflet-полигоном? Несколько фигур подходит под это описание. */
  def isLPolygon = false

}


/** Маркер-трейт leaflet-полигона. */
sealed trait ILPolygonGs extends IGeoShape {
  override def isLPolygon = true
}


/** Если элемент можно запрашивать в geo-shape search/filter, то об этом можно уведомить компилятор. */
sealed trait IGeoShapeQuerable extends IGeoShape


// Реализация GeoShape'ов:

object PointGs {
  implicit val POINT_GS_PICKLER: Pickler[PointGs] = {
    implicit val mGeoPointP = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[PointGs]
  }
}
/** Гео-шейп точки. */
case class PointGs(coord: MGeoPoint) extends IGeoShapeQuerable {
  override def shapeType = GsTypes.Point
  override def firstPoint = coord
  override def centerPoint = Some(coord)
}


/** Гео-шейп круга. */
// TODO Замёржить common-модель MGeoCircle в этот шейп.
object CircleGs {

  implicit val CIRCLE_GS_PICKLER: Pickler[CircleGs] = {
    implicit val mGeoPointP = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[CircleGs]
  }

  /** Валидация гео-круга под нужды какой-то абстрактной формы.  */
  def validate(gc: CircleGs, radiusConstrains: IMinMaxM): ValidationNel[String, CircleGs] = {
    val ePrefix = "e.adn.rad.radius.too"
    (
      MGeoPoint.validator( gc.center ) |@|
      Validation.liftNel(gc.radiusM)( _ < radiusConstrains.MIN_M.toDouble, s"$ePrefix.small" ) |@|
      Validation.liftNel(gc.radiusM)( _ > radiusConstrains.MAX_M.toDouble, s"$ePrefix.big")
    )( (_,_,_) => gc )
  }

}
case class CircleGs(
                     center   : MGeoPoint,
                     radiusM  : Double
                   )
  extends IGeoShapeQuerable
{
  // Internal API
  override def shapeType = GsTypes.Circle
  override def firstPoint = center
  override def centerPoint = Some(center)
  // API взято из MGeoCircle, которая была замёржена прямо сюда.
  def radiusKm = radiusM / 1000d
  def withCenter(center: MGeoPoint) = copy(center = center)
  def withRadiusM(radiusM: Double) = copy(radiusM = radiusM)
}


object EnvelopeGs {
  implicit def ENVELOPE_GS_PICKLER: Pickler[EnvelopeGs] = {
    implicit val mGeoPointP = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[EnvelopeGs]
  }
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
  // Занимается или нет область на карте -- это зависит только от шейпов внутри коллекции.
  override def isArea: Boolean = {
    geoms.exists(_.isArea)
  }
}
// TODO В целях безопасности, boopickle для GeometryCollectionGs генерится внутри IGeoShape с учётом рекурсии.


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
  implicit def MULTI_POINT_PICKLER: Pickler[MultiPointGs] = {
    implicit val mGeoPointP = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[MultiPointGs]
  }
}

/** Гео-шейп для ListString geometry. */
case class LineStringGs(coords: Seq[MGeoPoint]) extends MultiPointShape {
  override def shapeType = GsTypes.LineString
  override def firstPoint = coords.head
}
object LineStringGs extends IApply1 {
  override type ApplyArg_t = Seq[MGeoPoint]
  override type T = LineStringGs
  implicit val LINE_STRING_PICKLER: Pickler[LineStringGs] = {
    implicit val mGeoPointP = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[LineStringGs]
  }
}


/** Гео-шейп мульти-линии. */
case class MultiLineStringGs(lines: Seq[LineStringGs]) extends IGeoShapeQuerable {
  override def shapeType = GsTypes.MultiLineString
  override def firstPoint = lines.head.firstPoint
}
object MultiLineStringGs {
  implicit def MULTI_LINE_STRING_PICKLER: Pickler[MultiLineStringGs] = {
    implicit val lineStringGsP = LineStringGs.LINE_STRING_PICKLER
    generatePickler[MultiLineStringGs]
  }
}


/** Гео-шейп полигона с необязательными дырками в двумерном пространстве. */
case class PolygonGs(
                      outer : LineStringGs,
                      holes : List[LineStringGs] = Nil
                    )
  extends IGeoShapeQuerable
  with ILPolygonGs
{
  override def shapeType = GsTypes.Polygon
  override def firstPoint = outer.firstPoint
  def toMpGss = outerWithHoles.map(_.coords)
  def outerWithHoles = outer :: holes
}
object PolygonGs {
  implicit val POLYGON_GS_PICKLER: Pickler[PolygonGs] = {
    implicit val lineStringGsP = LineStringGs.LINE_STRING_PICKLER
    generatePickler[PolygonGs]
  }
}


/** Гео-шейп мульти-полигона. */
case class MultiPolygonGs(polygons: Seq[PolygonGs])
  extends IGeoShapeQuerable
  with ILPolygonGs
{
  override def shapeType = GsTypes.MultiPolygon
  override def firstPoint = polygons.head.firstPoint
}
object MultiPolygonGs {
  implicit val MULTI_POLYGON_GS_PICKLER: Pickler[MultiPolygonGs] = {
    implicit val polygonGsP = PolygonGs.POLYGON_GS_PICKLER
    generatePickler[MultiPolygonGs]
  }
}
