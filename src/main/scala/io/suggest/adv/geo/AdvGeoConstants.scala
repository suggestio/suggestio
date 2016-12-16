package io.suggest.adv.geo

import io.suggest.common.maps.MapFormConstants
import io.suggest.common.maps.rad.RadMapConstants
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

  /** Название form-поля для состояния карты. */
  def STATE_FN    = MapFormConstants.STATE_FN

  /** Название form-поля для состояния круга на карте. */
  def CIRCLE_FN   = RadMapConstants.CIRCLE_FN

  /** id инпута, содержащего ссылку на карту узлов-ресиверов. */
  def AD_ID_INPUT_ID = PREFIX + "ngu"


  object OnMainScreen {

    def FN = "oms"

    def ID = FORM_ID + QsConstants.KEY_PARTS_DELIM_STR + FN

  }


  /** Константы инфы о текущих гео-размещениях.
    * Для передачи данных в Leaflet используется GeoJSON FeatureCollection.
    * Feature.properties содержит инфу по рендеру кругов и попапов.
    * Featore.geometry содержит точки для кругов.
    */
  object GjFtPropsC {

    /** Имя поля радиуса круга. */
    def RADIUS_FN         = "r"

    /** Имя поля для содержимое попапа. */
    def POPUP_CONTENT_FN  = "p"

    /** Имя поля для цвета заливки геом.фигуры. */
    def FILL_COLOR_FN     = "f"

    /** Имя поля непрозрачности заливки шейпа. */
    def FILL_OPACITY_FN   = "o"

  }

  /** Константы заливки шейпов на карте гео-размещений. */
  object CurrShapes {

    /** id элемента, содержащего JSON-описалово текущих размещений. */
    def DATA_CONTAINER_ID = PREFIX + "csi"

    /** Цвет заливки запрошенных размещений. */
    def REQ_COLOR = "#4C88A9"

    /** Цвет заливки принятых размещений. */
    def OK_COLOR  = "#1ABC8A"

    /** Прозрачность заливки info-шейпов. */
    def OPACITY   = 0.5F

  }


  /** Названия полей в properties, касающихся свойст отображаемого узла. */
  object AdnNodes {

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

      /** Имя поля формы с группами узлов. */
      final val GROUPS_FN         = "a"

      /** id группы узлов, алиас некой общей сути у узлов.
        * Например и изначально -- это тип узлов (ntype), но совсем не обязательно. */
      final val GROUP_ID_FN       = "b"

      /** Имя поля с узлами. Например, внутри группы. */
      final val NODES_FN          = "c"

      /** id узла. */
      final val NODE_ID_FN        = "d"

      /** Имя поля флага направления действия: созидание или разрушение? */
      final val IS_CREATE_FN      = "e"

      /** Поставлена ли галочка? */
      final val CHECKED_FN        = "f"

      /** Имя поля названия узла/группы.  */
      final val NAME_FN           = "g"

      /** Находится ли узел в онлайне прямо сейчас? */
      final val IS_ONLINE_NOW_FN  = "h"

      /** Интервал дат размещения. */
      final val INTERVAL_FN       = "i"


      private def ID_DELIM        = QsConstants.KEY_PARTS_DELIM_STR

      /** префикс DOM-id контейнера данных для попапа ресивера. */
      def CONT_ID_PREFIX           = "prc" + ID_DELIM

      /** префикс DOM-id контейнера данных одной группы для ресивера. */
      def GRP_CONT_ID_PREFIX       = "g" + CONT_ID_PREFIX
      def GRP_NODES_CONT_ID_PREFIX = "s" + GRP_CONT_ID_PREFIX

      /** Префикс DOM-id для контейнера одного под-узла в рамках группы узлов одного ресивера. */
      def NODE_GRP_CONT_ID_PREFIX  = "n" + GRP_CONT_ID_PREFIX

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
