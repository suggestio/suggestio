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

    /** id инпута, по которому доступна вся карта узлов системы в GeoJSON. */
    def ALL_URL_INPUT_ID = "allscnm"
    def ALL_NODES_SRC_ID = ALL_URL_INPUT_ID

    /** Имя поля в Feature.props с числом, описывающим кол-во узлов в заданной области. */
    def COUNT_FN  = "c"

    /** Имя поля, содержащего массив слоёв, подлежащих рендеру. */
    final val SOURCES_FN = "s"

    /** Имя сорса для mapbox, унифицированное на клиенте и сервере. */
    final val SRC_NAME_FN     = "n"

    /** Название поля со значением флага кластеризованности слоя. */
    final val IS_CLUSTERED_FN = "u"

    /** Название поля GeoJSON-фич слоя. */
    final val SRC_DATA_FN     = "g"


    /** Названия сорсов mapbox. */
    object Sources {

      private def PREFIX = SOURCES_FN + "-"

      /** Source точек узлов. */
      def POINTS    = PREFIX + "pt"

      /** Source кластеров узлов. */
      def CLUSTERS  = PREFIX + "cl"

      def ALL_SOURCES = Seq(POINTS, CLUSTERS)

      /** Имя layer'а для отображения надписей. */
      def CLUSTER_LABELS = CLUSTERS + "-lbl"

      def FILL_COLOR = "#FFFFFF"
      def POINT_RADIUS_PX   = 3
      def CLUSTER_RADIUS_PX = 10

    }

  }


  /** Константы запроса запрос карты узлов. */
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

      def ENVELOPE_TOP_LEFT         = ENVELOPE_FN + MAP_DELIM + TOP_LEFT_FN
      def ENVELOPE_TOP_LEFT_LON     = ENVELOPE_TOP_LEFT + MAP_DELIM + LON_FN
      def ENVELOPE_TOP_LEFT_LAT     = ENVELOPE_TOP_LEFT + MAP_DELIM + LAT_FN

      def ENVELOPE_BOTTOM_RIGHT     = ENVELOPE_FN + MAP_DELIM + BOTTOM_RIGHT_FN
      def ENVELOPE_BOTTOM_RIGHT_LON = ENVELOPE_BOTTOM_RIGHT + MAP_DELIM + LON_FN
      def ENVELOPE_BOTTOM_RIGHT_LAT = ENVELOPE_BOTTOM_RIGHT + MAP_DELIM + LAT_FN

    }

  }

}
