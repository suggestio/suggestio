package io.suggest.geo

import io.suggest.common.qs.QsConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 15:43
 * Description: Константы геолокации.
 */
object GeoConstants {

  /** id режима геолокации  */
  def GEO_MODE_IP = "ip"

  /** Контейнер констант первого поколения [[GeoConstants]]. */
  object Qs {

    /** Разделитель. */
    def DELIM             = QsConstants.KEY_PARTS_DELIM_STR

    /** Географическая широта. */
    def LAT_FN            = "a"
    /** Географическая долгота. */
    def LON_FN            = "o"

    def TOP_LEFT_FN       = "t"
    def BOTTOM_RIGHT_FN   = "b"

  }

  /** Данные геолокации заворачиваются сюда. */
  object GeoLocQs {

    /** Центр круга, описывающего данные геолокации устройства. */
    def CENTER_FN         = "e"

    /** Радиус круга, описывающего данные геолокации устройства. */
    def ACCURACY_M_FN       = "u"

  }

}
