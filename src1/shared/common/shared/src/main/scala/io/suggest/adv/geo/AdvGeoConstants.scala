package io.suggest.adv.geo

import io.suggest.common.maps.rad.IMinMaxM

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 17:43
  * Description: Константы страницы/формы размещения карточки в гео-тегах.
  */
object AdvGeoConstants {

  def PREFIX = "ag"

  /** id формы размещения в гео-месте. */
  def FORM_ID = "f" + PREFIX

  /** id элемента, в который будет отрендерена react-форма. */
  def REACT_FORM_TARGET_ID = PREFIX + "rft"

  object Radius extends IMinMaxM {

    override def MIN_M = 5

    override def MAX_M = 1000000

  }


  /** Константы инфы о текущих гео-размещениях.
    * Для передачи данных в Leaflet используется GeoJSON FeatureCollection.
    * Feature.properties содержит инфу по рендеру кругов и попапов.
    * Featore.geometry содержит точки для кругов.
    */
  object GjFtPropsC {

    /** Имя поля радиуса круга. */
    final val CIRCLE_RADIUS_M_FN    = "r"

    /** id item'а шейпа. Это id, по которому можно получить доступ ко всем остальным id. */
    final val ITEM_ID_FN            = "i"

    /** Есть ли хоть одно подтвержденное размещение? */
    final val HAS_APPROVED_FN       = "a"

  }


  /** Названия полей в properties, касающихся свойст отображаемого узла. */
  object AdnNodes {

    /** Максимальное кол-во изменений галочек за один раз. */
    def MAX_RCVRS_PER_TIME = 100

  }

}
