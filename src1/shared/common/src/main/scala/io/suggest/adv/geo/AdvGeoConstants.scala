package io.suggest.adv.geo

import io.suggest.common.maps.rad.IMinMaxM
import io.suggest.common.qs.QsConstants

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
    final val CIRCLE_RADIUS_M_FN   = "r"

    /** id item'а шейпа. Это id, по которому можно получить доступ ко всем остальным id. */
    final val ITEM_ID_FN        = "i"

    /** Есть ли хоть одно подтвержденное размещение? */
    final val HAS_APPROVED_FN   = "a"

  }

  /** Константы заливки шейпов на карте гео-размещений. */
  object CurrShapes {

    /** Цвет заливки запрошенных размещений. */
    def REQ_COLOR = "#4C88A9"

    /** Цвет заливки принятых размещений. */
    def OK_COLOR  = "#1ABC8A"

    /** Прозрачность заливки info-шейпов. */
    def OPACITY   = 0.5

  }


  /** Названия полей в properties, касающихся свойст отображаемого узла. */
  object AdnNodes {

    /** Максимальное кол-во изменений галочек за один раз. */
    def MAX_RCVRS_PER_TIME = 100

    /** Имя поля с подсказкой узла. */
    def HINT_FN       = "h"

    /** Имя поля с именем узла. */
    def NODE_ID_FN    = "n"

    /** Имя поля с цветом фона (полезно для рендера логотипа с прозрачностью, например). */
    def BG_COLOR_FN   = "b"

    def ICON_FN       = "i"

    object Icon {

      /** Имя поля с адресом иконки узла. */
      def URL_FN    = "u"

      def WIDTH_FN  = "w"
      def HEIGHT_FN = "h"

    }

    /** Константы для попапа, выпрыгивающего при клике по узлу. */
    object Popup {

      /** id группы узлов, алиас некой общей сути у узлов.
        * Например и изначально -- это тип узлов (ntype), но совсем не обязательно. */
      final val GROUP_ID_FN       = "b"

      /** Поставлена ли галочка? */
      final val CHECKED_FN        = "f"


      private def ID_DELIM        = QsConstants.KEY_PARTS_DELIM_STR

      /** префикс DOM-id контейнера данных для попапа ресивера. */
      def CONT_ID_PREFIX           = "prc" + ID_DELIM

    }


    /** Константы полей формы для размещения на узлах-ресиверах. */
    object Req {

      def RCVR_FN     = "rcvr"

      def FROM_FN     = NODE_ID_FN
      def TO_FN       = "t"

      def GROUP_ID_FN = Popup.GROUP_ID_FN
      def VALUE_FN    = Popup.CHECKED_FN

    }

  }

}
