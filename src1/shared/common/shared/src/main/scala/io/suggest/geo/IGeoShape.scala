package io.suggest.geo

import boopickle.Default._
import io.suggest.common.maps.rad.IMinMaxM
import io.suggest.primo.IApply1
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._   // НАДО!

import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._
import play.api.libs.json._

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


  /** Контейнер различных вариантов поддержки JSON-сериализации/десериализации.
    *
    */
  object JsonFormats {

    /** Самая главная и самая обычная форматировалка любых GeoShape'ов.
      * Заточенна под Elasticsearch, но ДОЛЖНА использоваться во всех хранилищах.
      * Когда не ясно, какой форматтер использовать, надо использовать этот.
      */
    implicit lazy val allStoragesEsFormat: OFormat[IGeoShape] = {
      MGsJsonFormatter(
        gsFieldNames   = IGsFieldNames.Es,
        gsTypeFormat   = GsType.GS_TYPE_FORMAT,
        geoPointFormat = MGeoPoint.JsonFormatters.ARRAY_OR_ES_OBJECT
      )
        .geoShape
    }

    /** GeoJSON-only-форматтер.
      *
      * FIXME На GeoJSON-несовместимых GsType обязательно будут экзепшены.
      */
    implicit def geoJsonFormat: OFormat[IGeoShape] = {
      MGsJsonFormatter(
        gsFieldNames   = IGsFieldNames.Es,
        // TODO Опасно: будет экзепшен, если на вход был подан GeoJSON-НЕсовместимый гео-шейп.
        gsTypeFormat   = GsType.gsTypeGeoJsonCompatibleFormat.asInstanceOf[Format[GsType]],
        geoPointFormat = MGeoPoint.FORMAT_GEO_ARRAY
      )
        .geoShape
    }


    /** Внутренний sio JSON, минимальный по максимуму. Во благо оптимизации, здесь можно ломать совместимость. */
    implicit def internalMinFormat: OFormat[IGeoShape] = {
      MGsJsonFormatter(
        gsFieldNames    = IGsFieldNames.Minimal,
        gsTypeFormat    = GsType.GS_TYPE_FORMAT,
        geoPointFormat  = MGeoPoint.FORMAT_GEO_ARRAY
      )
        .geoShape
    }

  }

  @inline implicit def univEq: UnivEq[IGeoShape] = UnivEq.derive


  /** Вернуть объект-компаньон для указанного типа шейпа. */
  def companionFor(gsType: GsType): IGeoShapeCompanion[_ <: IGeoShape] = {
    gsType match {
      case GsTypes.Circle               => CircleGs
      case GsTypes.Polygon              => PolygonGs
      case GsTypes.Point                => PointGs
      case GsTypes.LineString           => LineStringGs
      case GsTypes.Envelope             => EnvelopeGs
      case GsTypes.MultiLineString      => MultiLineStringGs
      case GsTypes.MultiPoint           => MultiPointGs
      case GsTypes.MultiPolygon         => MultiPolygonGs
      case GsTypes.GeometryCollection   => GeometryCollectionGs
    }
  }

  /** Вернуть объект-компаньон для указанного шейпа. */
  def companionFor[T <: IGeoShape](gs: T): IGeoShapeCompanion[T] = {
    companionFor(gs.shapeType)
      // Можно без asInstanceOf, но придётся написать ещё один длинный match, как соседнем companionFor().
      .asInstanceOf[IGeoShapeCompanion[T]]
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
    shapeType.geoJsonNameOpt
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



/** Интерфейс gs-компаньона (объекта). */
sealed trait IGeoShapeCompanion[T <: IGeoShape]


/** Маркер-трейт leaflet-полигона. */
sealed trait ILPolygonGs extends IGeoShape {
  override def isLPolygon = true
}


/** Если элемент можно запрашивать в geo-shape search/filter, то об этом можно уведомить компилятор. */
sealed trait IGeoShapeQuerable extends IGeoShape


// Реализация GeoShape'ов:

object PointGs extends IGeoShapeCompanion[PointGs] {
  implicit val POINT_GS_PICKLER: Pickler[PointGs] = {
    implicit val mGeoPointP = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[PointGs]
  }
  @inline implicit def univEq: UnivEq[PointGs] = UnivEq.derive
}
/** Гео-шейп точки. */
case class PointGs(coord: MGeoPoint) extends IGeoShapeQuerable {
  override def shapeType = GsTypes.Point
  override def firstPoint = coord
  override def centerPoint = Some(coord)
}


/** Гео-шейп круга. */
object CircleGs extends IGeoShapeCompanion[CircleGs] {

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

  @inline implicit def univEq: UnivEq[CircleGs] = UnivEq.derive

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


object EnvelopeGs extends IGeoShapeCompanion[EnvelopeGs] {
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
object GeometryCollectionGs extends IGeoShapeCompanion[GeometryCollectionGs] {
  @inline implicit def univEq: UnivEq[GeometryCollectionGs] = UnivEq.derive
  // TODO В целях безопасности, boopickle для GeometryCollectionGs генерится внутри IGeoShape с учётом рекурсии.
}


/** Общий интерфейс для [[LineStringGs]] и [[MultiPointGs]] обитает здесь. */
sealed trait MultiPointShape extends IGeoShapeQuerable {
  def coords: Seq[MGeoPoint]
  override def firstPoint = coords.head
}

/** Трейт для объекта-компаньона, содержащий общий код между multipoint-шейпами. */
sealed trait MultiPointShapeStaticC[Gs_t <: MultiPointShape] extends IGeoShapeCompanion[Gs_t] with IApply1 {
  override type ApplyArg_t = Seq[MGeoPoint]
  override type T = Gs_t
}

/** Гео-шейп для MultiPoint geometry. */
case class MultiPointGs(coords: Seq[MGeoPoint]) extends MultiPointShape {
  override def shapeType = GsTypes.MultiPoint
}
object MultiPointGs extends MultiPointShapeStaticC[MultiPointGs] {
  implicit def MULTI_POINT_PICKLER: Pickler[MultiPointGs] = {
    implicit val mGeoPointP = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[MultiPointGs]
  }
  @inline implicit def univEq: UnivEq[MultiPolygonGs] = UnivEq.derive
}

/** Гео-шейп для ListString geometry. */
case class LineStringGs(coords: Seq[MGeoPoint]) extends MultiPointShape {
  override def shapeType = GsTypes.LineString
  override def firstPoint = coords.head
}
object LineStringGs extends MultiPointShapeStaticC[LineStringGs] {
  implicit def LINE_STRING_PICKLER: Pickler[LineStringGs] = {
    implicit val mGeoPointP = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[LineStringGs]
  }
  @inline implicit def univEq: UnivEq[LineStringGs] = UnivEq.derive
}


/** Гео-шейп мульти-линии. */
case class MultiLineStringGs(lines: Seq[LineStringGs]) extends IGeoShapeQuerable {
  override def shapeType = GsTypes.MultiLineString
  override def firstPoint = lines.head.firstPoint
}
object MultiLineStringGs extends IGeoShapeCompanion[MultiLineStringGs] {
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
object PolygonGs extends IGeoShapeCompanion[PolygonGs] {
  implicit val POLYGON_GS_PICKLER: Pickler[PolygonGs] = {
    implicit val lineStringGsP = LineStringGs.LINE_STRING_PICKLER
    generatePickler[PolygonGs]
  }
  @inline implicit def univEq: UnivEq[PolygonGs] = UnivEq.derive

  def apply(lsgss: List[Seq[MGeoPoint]]): PolygonGs = {
    PolygonGs(
      outer = LineStringGs( lsgss.head ),
      holes = lsgss.tail.map(LineStringGs.apply)
    )
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
object MultiPolygonGs extends IGeoShapeCompanion[MultiPolygonGs] {
  implicit val MULTI_POLYGON_GS_PICKLER: Pickler[MultiPolygonGs] = {
    implicit val polygonGsP = PolygonGs.POLYGON_GS_PICKLER
    generatePickler[MultiPolygonGs]
  }

  @inline implicit def univEq: UnivEq[MultiPolygonGs] = UnivEq.derive

}
