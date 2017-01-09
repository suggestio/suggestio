package util.maps

import com.google.inject.Singleton
import models.maps.MapViewState
import play.api.data._, Forms._
import io.suggest.common.maps.MapFormConstants._
import util.FormUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:47
  * Description: Утиль для форм с какой-то картой.
  */
@Singleton
class MapFormUtil {

  val centerKM  = CENTER_FN  -> FormUtil.geoPointM

  /** Маппинг значения зума карты. */
  def mapZoomM: Mapping[Int] = number(min = 0, max = 20)

  /** Маппинг состояния карты. Надо вынести его отсюда куда-нибудь. */
  def mapStateM: Mapping[MapViewState] = {
    mapping(
      centerKM,
      ZOOM_FN    -> mapZoomM
    )
    { MapViewState.apply }
    { MapViewState.unapply }
  }

}
