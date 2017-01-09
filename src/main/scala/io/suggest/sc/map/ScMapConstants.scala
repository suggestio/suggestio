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
    def ALL_URL_INPUT_ID          = "allscnm"

    /** id сорса, в который заливается карта вообще всех узлов. */
    def ALL_NODES_SRC_ID          = "ans"

    object Layers {

      /** Префикс слоя. */
      private def ID_PREFIX         = "nl"

      /** Имя слоя, где отображаются некластеризованные точки. */
      def NON_CLUSTERED_LAYER_ID          = "n" + ID_PREFIX
      def NON_CLUSTERED_LAYER_LABELS_ID   = NON_CLUSTERED_LAYER_ID + "l"

      /** Префикс названий слоёв,  */
      def CLUSTERED_LAYER_ID_PREFIX = "c" + ID_PREFIX

      /** Сборка имени слоя с учетом индекса. */
      def clusterLayerId(i: Int): String = {
        CLUSTERED_LAYER_ID_PREFIX + i
      }

      /** Слой с цифрами-количествами. */
      def COUNT_LABELS_LAYER_ID     = "r" + ID_PREFIX

    }


    /** Названия сорсов mapbox. */
    object Sources {

      def FILL_COLOR = "#FFFFFF"
      def MARKER_RADIUS_PX   = 18

    }

  }

}
