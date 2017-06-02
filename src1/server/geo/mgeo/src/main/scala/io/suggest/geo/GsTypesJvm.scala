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
    gsType match {
      case GsTypes.Polygon            => PolygonGs
      case GsTypes.Circle             => CircleGs
      case GsTypes.Point              => PointGs
      case GsTypes.GeometryCollection => GeometryCollectionGs
      case GsTypes.Envelope           => EnvelopeGs
      case GsTypes.LineString         => LineStringGs
      case GsTypes.MultiLineString    => MultiLineStringGs
      case GsTypes.MultiPoint         => MultiPointGs
      case GsTypes.MultiPolygon       => MultiPolygonGs
    }
  }

}
