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
  @deprecated("use loc env option None instead", "2016.sep.16")
  def GEO_MODE_IP = "ip"


  /** Географические константы для планеты. */
  object Earth {

    /**
      * Если считать геойд сферой, то сфера будет иметь указанный радиус.
      * @return Радиус сферической Земли в метрах.
      */
    def RADIUS_M = 6378137

  }


  /** Контейнер констант первого поколения [[GeoConstants]]. */
  object Qs {

    /** Разделитель. */
    def DELIM             = QsConstants.KEY_PARTS_DELIM_STR

    def TOP_LEFT_FN       = "t"
    def BOTTOM_RIGHT_FN   = "b"

    /** При сериализации точки в строку, используется этот разделитель широты и долготы. */
    def LAT_LON_DELIM_FN  = '|'

  }

  /** Данные геолокации заворачиваются сюда. */
  object GeoLocQs {

    /** Центр круга, описывающего данные геолокации устройства. */
    def CENTER_FN         = "e"

    /** Радиус круга, описывающего данные геолокации устройства. */
    def ACCURACY_M_FN     = "u"

  }

}
