package io.suggest.geo

import io.suggest.primo.IApply1
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.03.18 14:50
  * Description: Конфигурация для рендера
  */

trait IGsFieldNames {
  def COORDS_ESFN: String
  def TYPE_ESFN: String
  def RADIUS_ESFN: String
  def GEOMETRIES_ESFN: String
}


object IGsFieldNames {

  /** Нормальные названия полей, соответствуют E. */
  case object Es extends IGsFieldNames {
    override val COORDS_ESFN        = "coordinates"
    override val TYPE_ESFN          = "type"
    override val RADIUS_ESFN        = "radius"
    override val GEOMETRIES_ESFN    = "geometries"
  }

  /** Названия полей минимально-возможной длины. Только для внутреннего использования sio.
    * Появились для оптимального трансфера данных с сервера в карту ресиверов.
    */
  case object Minimal extends IGsFieldNames {
    override val COORDS_ESFN        = "c"
    override val TYPE_ESFN          = "t"
    override val RADIUS_ESFN        = "r"
    override val GEOMETRIES_ESFN    = "g"
  }

}

/** Система JSON Format для [[IGeoShape]] и gs-реализаций.
  *
  * Появилась, т.к. есть необходимость по-разному рендерить в JSON одни и те же вещи.
  * Например, на сервере часто использовуется MGeoPoint в виде JsArray,
  * а для MGeoPoint в QS требуется JsObject с укороченными названиями полей. Или строка даже.
  *
  * Так же, здесь предусмотрена возможность кэширования используемых инстансов.
  * Это особенно нужно на клиенте: парсинг длинных JSON-ответов сервера позволит
  * минимизировать расходование ресурсов клиентского устройства.
  *
  * @param gsFieldNames Настройки JSON-рендера IGeoShape: имена полей.
  * @param gsTypeFormat Какие названия рендерить для типа шейпа: ES или GeoJSON.
  * @param geoPointFormat JSON-рендер GeoPoint.
  */
case class MGsJsonFormatter(
                             gsFieldNames                 : IGsFieldNames,
                             implicit val gsTypeFormat    : Format[GsType],
                             implicit val geoPointFormat  : Format[MGeoPoint]
                           ) {

  implicit lazy val point: OFormat[PointGs] = {
    (__ \ gsFieldNames.COORDS_ESFN)
      .format[MGeoPoint]
      .inmap(PointGs.apply, unlift(PointGs.unapply))
  }

  implicit lazy val circle: OFormat[CircleGs] = (
    (__ \ gsFieldNames.COORDS_ESFN).format[MGeoPoint] and
    (__ \ gsFieldNames.RADIUS_ESFN).format(MDistance.MDISTANCE_FORMAT)
  )(CircleGs.apply, unlift(CircleGs.unapply))

  implicit lazy val polygon: OFormat[PolygonGs] = {
    (__ \ gsFieldNames.COORDS_ESFN)
      .format[List[Seq[MGeoPoint]]]
      .inmap [PolygonGs] (
        PolygonGs.apply,
        _.toMpGss
      )
  }

  implicit lazy val envelope: OFormat[EnvelopeGs] = {
    (__ \ gsFieldNames.COORDS_ESFN)
      .format[Seq[MGeoPoint]]
      .inmap[EnvelopeGs] (
        { case Seq(c1, c3) =>
            EnvelopeGs(c1, c3)
          case other =>
            throw new IllegalArgumentException("Invalid envelope coords count: " + other)
        },
        {egs =>
          egs.topLeft :: egs.bottomRight :: Nil
        }
      )
  }

  implicit lazy val multiLineString: OFormat[MultiLineStringGs] = {
    (__ \ gsFieldNames.COORDS_ESFN)
      .format[Seq[Seq[MGeoPoint]]]
      .inmap [MultiLineStringGs] (
        { ss => MultiLineStringGs( ss.map(LineStringGs.apply)) },
        { _.lines.map(_.coords) }
      )
  }

  def _multiPointDataFormat[Gs_t <: MultiPointShape](companion: IApply1 { type ApplyArg_t = Seq[MGeoPoint]; type T = Gs_t }): OFormat[Gs_t] = {
    (__ \ gsFieldNames.COORDS_ESFN)
      .format[Seq[MGeoPoint]]
      .inmap[Gs_t](companion.apply, _.coords)
  }

  implicit lazy val lineString: OFormat[LineStringGs] = {
    _multiPointDataFormat( LineStringGs )
  }
  implicit lazy val multiPoint: OFormat[MultiPointGs] = {
    _multiPointDataFormat( MultiPointGs )
  }

  implicit lazy val multiPolygon: OFormat[MultiPolygonGs] = {
    (__ \ gsFieldNames.COORDS_ESFN)
      .format[Seq[List[Seq[MGeoPoint]]]]
      .inmap [MultiPolygonGs] (
        {polyGss =>
          MultiPolygonGs(
            for (polyCoords <- polyGss) yield {
              PolygonGs(polyCoords)
            }
          )
        },
        {mpGs =>
          mpGs.polygons
            .map { _.toMpGss }
        }
      )
  }

  implicit lazy val geometryCollection: OFormat[GeometryCollectionGs] = {
    (__ \ gsFieldNames.GEOMETRIES_ESFN)
      .lazyFormat( implicitly[Format[Seq[IGeoShape]]] )
      .inmap[GeometryCollectionGs](GeometryCollectionGs.apply, _.geoms)
  }

  lazy val _gsTypeOFormat: OFormat[GsType] = {
    (__ \ gsFieldNames.TYPE_ESFN).format(gsTypeFormat)
  }

  implicit lazy val geoShape: OFormat[IGeoShape] = {
    // Десериализация:
    val reads = for {
      gsType <- _gsTypeOFormat
      gs     <- forType(gsType)
    } yield {
      gs: IGeoShape
    }

    // Сериализация:
    val writes = OWrites[IGeoShape] { gs =>
      val gsTypeJsObj = _gsTypeOFormat.writes(gs.shapeType)
      val dataJsObj = forGs(gs)
        .writes(gs)
      gsTypeJsObj ++ dataJsObj
    }

    OFormat[IGeoShape]( reads, writes )
  }

  def forType(gsType: GsType): OFormat[_ <: IGeoShape] = {
    gsType match {
      case GsTypes.Circle               => circle
      case GsTypes.Polygon              => polygon
      case GsTypes.Point                => point
      case GsTypes.LineString           => lineString
      case GsTypes.Envelope             => envelope
      case GsTypes.MultiLineString      => multiLineString
      case GsTypes.MultiPoint           => multiPoint
      case GsTypes.MultiPolygon         => multiPolygon
      case GsTypes.GeometryCollection   => geometryCollection
    }
  }

  def forGs[T <: IGeoShape](gs: T): OFormat[T] = {
    forType( gs.shapeType )
      // Можно без asInstanceOf, но придётся написать ещё один длинный match, как соседнем companionFor().
      .asInstanceOf[OFormat[T]]
  }


}
