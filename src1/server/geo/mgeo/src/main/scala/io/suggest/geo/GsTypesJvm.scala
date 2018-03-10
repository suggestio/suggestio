package io.suggest.geo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.06.17 18:47
  * Description: Серверная утиль для кросс-платформенной enum-модели GsTypes.
  */
object GsTypesJvm {

  /** Раньше метод обращения к объекту-компаньону жил прямо в GsType.
    *
    * Теперь, он отдельно и имеет сложность O(9), в среднем O(2), вместо исходного O(1),
    * но зато сама модель стала кроссплатформенной!
    *
    * @param gsType GeoShape Type.
    * @return Инстанс объекта-компаньона для абстрактного гео-шейпа.
    */
  def jvmCompanionFor(gsType: GsType): GsStaticJvm = {
    val cqOpt: Option[GsStaticJvm] = esQuerableJvmCompanionFor(gsType)
    cqOpt.getOrElse {
      gsType match {
        case GsTypes.GeometryCollection =>
          GeometryCollectionGsJvm
        // should never happen:
        case other =>
          throw new NotImplementedError(s"Looks like, new gsType=$other exist in code, but not implemented in ${getClass.getName}")
      }
    }
  }


  /** Вернуть опциональный объект-компаньон для указанного querable-типа шейпов.
    *
    * @param gsType Querable-тип шейпа.
    * @return Опциональный объект-компаньон.
    *         None, если тип шейпа не относится к querable-шейпам.
    */
  def esQuerableJvmCompanionFor(gsType: GsType): Option[GsStaticJvmQuerable] = {
    // Этот метод используется только в jvmCompanionFor(). Можно замёржить.
    val cOrNull: GsStaticJvmQuerable = gsType match {
      case GsTypes.Polygon            => PolygonGsJvm
      case GsTypes.Circle             => CircleGsJvm
      case GsTypes.Point              => PointGsJvm
      case GsTypes.Envelope           => EnvelopeGsJvm
      case GsTypes.LineString         => LineStringGsJvm
      case GsTypes.MultiLineString    => MultiLineStringGsJvm
      case GsTypes.MultiPoint         => MultiPointGsJvm
      case GsTypes.MultiPolygon       => MultiPolygonGsJvm
      case _                          => null
    }
    Option( cOrNull )
  }

}
