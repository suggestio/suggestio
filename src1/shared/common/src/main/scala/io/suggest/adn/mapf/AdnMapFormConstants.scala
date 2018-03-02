package io.suggest.adn.mapf

import io.suggest.common.maps.rad.IMinMaxM

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:42
  * Description: Константы для формы/страницы размещения ADN-узла на карте.
  */
object AdnMapFormConstants {

  def ID_PREFIX = "amf_"

  /** id контейнера react-формы. */
  def FORM_CONT_ID = ID_PREFIX + "C"


  /** Дефолтовый zoom карты. */
  def MAP_ZOOM_DFLT = 14


  /** Константы RAD-компонента. */
  object Rad {

    /** Константы радиуса. */
    object RadiusM extends IMinMaxM {

      /** Минимальный радиус в метрах. */
      override def MIN_M = 5

      /** Дефолтовое значение радиуса. */
      def DEFAULT = 30

      /** Максимальное значение радиуса. */
      override def MAX_M = 400

    }

  }

}
