package io.suggest.common.maps.mapbox

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 18:29
  * Description: Константы гео-карты mapbox в выдаче.
  */
object MapBoxConstants {

  def SUFFIX = "mab"

  /** id input-элемента, в котором сервер сообщает access token для доступа к карте. */
  def ACCESS_TOKEN_INPUT_ID = "acto" + SUFFIX

  /** Обыденные названия слоёв разных. */
  object Layers {

    def MY_GEOLOC_LAYER_ID = "a"

    object MyGeoLoc {

      def INNER_RADIUS_PX = 5

      def INNER_COLOR     = "#007cbf"

    }


    object Circle {

      object Paint {

        def PREFIX = "circle-"

        def RADIUS    = PREFIX + "radius"
        def COLOR     = PREFIX + "color"
        def BLUR      = PREFIX + "blur"
        def OPACITY   = PREFIX + "opacity"
        def TRANSLATE = PREFIX + "translate"
        def TRANSLATE_ANCHOR = TRANSLATE + "-anchor"

      }

    }

  }

}
