package io.suggest.geo

import io.suggest.enum.EnumeratumJvmUtil
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.06.17 18:47
  * Description: Серверная утиль для кросс-платформенной enum-модели GsTypes.
  */
object GsTypesJvm {

  /** Поддержка JSON сериализации/десериализации в JsString. */
  implicit val GS_TYPE_FORMAT: Format[GsType] = {
    EnumeratumJvmUtil.enumEntryFormat(GsTypes)
  }


  /** Раньше метод обращения к объекту-компаньону жил прямо в GsType.
    *
    * Теперь, он отдельно и имеет сложность O(9), в среднем O(2), вместо исходного O(1),
    * но зато сама модель стала кроссплатформенной!
    *
    * @param gsType GeoShape Type.
    * @return Инстанс объекта-компаньона для абстрактного гео-шейпа.
    */
  def companionFor(gsType: GsType): GsStaticJvm = {
    val cqOpt: Option[GsStaticJvm] = esQuerableCompanionFor(gsType)
    cqOpt.getOrElse {
      gsType match {
        case GsTypes.GeometryCollection =>
          GeometryCollectionGs
        // should never happen:
        case other =>
          throw new NotImplementedError(s"Looks like, new gsType=$other exist in code, but not implemented in ${getClass.getName}")
      }
    }
  }


  def esQuerableCompanionFor(gsType: GsType): Option[GsStaticJvmQuerable] = {
    val cOrNull: GsStaticJvmQuerable = gsType match {
      case GsTypes.Polygon            => PolygonGs
      case GsTypes.Circle             => CircleGsJvm
      case GsTypes.Point              => PointGsJvm
      case GsTypes.Envelope           => EnvelopeGs
      case GsTypes.LineString         => LineStringGs
      case GsTypes.MultiLineString    => MultiLineStringGs
      case GsTypes.MultiPoint         => MultiPointGs
      case GsTypes.MultiPolygon       => MultiPolygonGs
      case _                          => null
    }
    Option( cOrNull )
  }

}
