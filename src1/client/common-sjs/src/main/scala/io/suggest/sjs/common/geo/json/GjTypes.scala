package io.suggest.sjs.common.geo.json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 21:39
  * Description: Идентификаторы типов
  */
object GjTypes {

  def FEATURE = "Feature"

  def FEATURE_COLLECTION = FEATURE + "Collection"

  object Geom {

    def POINT = "Point"

    def POLYGON = "Polygon"

  }

}
