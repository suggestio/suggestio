package io.suggest.geo.json

import io.suggest.i18n.MsgCodes

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

    def _MULTI = MsgCodes.`Multi`

    def MULTI_POLYGON = _MULTI + POLYGON

  }

}
