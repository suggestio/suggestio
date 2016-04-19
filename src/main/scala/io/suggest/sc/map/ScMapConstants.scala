package io.suggest.sc.map

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 18:40
  * Description: Константы для карты выдачи.
  */
object ScMapConstants {

  /** Константы для отображения узлов на карте или их кластеризованных аналогов. */
  object Nodes {

    /** Имя поля в Feature.props с числом, описывающим кол-во узлов в заданной области. */
    def COUNT_FN  = "c"



  }

  object Mqs {

    /** Название поля с описанием области карты. */
    def AREA_INFO_FN  = "m"

    /** Названием поля зума области карты (m.z). */
    def ZOOM_FN       = "z"
    /** Название поля с описанием отображаемого участка карты (m.e). */
    def ENVELOPE_FN   = "e"

    /** Генераторы полных имён qs-параметров, т.к. jsRouter сам этого делать не умеет, равно как и sc-sjs. */
    object Full {
      import io.suggest.geo.GeoConstants.Qs._

      def MAP_DELIM = DELIM
      def AREA_ENVELOPE = AREA_INFO_FN + MAP_DELIM + ENVELOPE_FN
      def AREA_ENVELOPE_TOP_LEFT = AREA_ENVELOPE + MAP_DELIM + TOP_LEFT_FN
      def AREA_ENVELOPE_TOP_LEFT_LON = AREA_ENVELOPE_TOP_LEFT + MAP_DELIM + LON_FN
      def AREA_ENVELOPE_TOP_LEFT_LAT = AREA_ENVELOPE_TOP_LEFT + MAP_DELIM + LAT_FN
      def AREA_ENVELOPE_BOTTOM_RIGHT = AREA_ENVELOPE + MAP_DELIM + BOTTOM_RIGHT_FN
      def AREA_ENVELOPE_BOTTOM_RIGHT_LON = AREA_ENVELOPE_BOTTOM_RIGHT + MAP_DELIM + LON_FN
      def AREA_ENVELOPE_BOTTOM_RIGHT_LAT = AREA_ENVELOPE_BOTTOM_RIGHT + MAP_DELIM + LAT_FN
      def AREA_ZOOM = AREA_INFO_FN + MAP_DELIM + ZOOM_FN
    }

  }

}
