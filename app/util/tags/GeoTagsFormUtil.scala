package util.tags

import com.google.inject.{Singleton, Inject}
import io.suggest.model.geo.{Distance, CircleGs}
import models.adv.gtag.{GtForm_t, MAdvFormResult}
import models.maps.MapViewState
import org.elasticsearch.common.unit.DistanceUnit
import play.api.data.{Mapping, Form}
import play.api.data.Forms._
import util.FormUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 21:45
 * Description: Утиль для формы заказа геотегов.
 */
@Singleton
class GeoTagsFormUtil @Inject() (
  tagsEditFormUtil: TagsEditFormUtil
) {

  // TODO В ожидании DI.
  private def formUtil = FormUtil

  val centerM  = "center"  -> formUtil.geoPointM

  /** Маппинг состояния карты. Надо вынести его отсюда куда-нибудь. */
  def mapStateM: Mapping[MapViewState] = {
    mapping(
      centerM,
      "zoom"    -> formUtil.mapZoomM
    )
    { MapViewState.apply }
    { MapViewState.unapply }
  }

  // TODO Разрешить нецелый километраж?
  def radiusM: Mapping[Distance] = {
    formUtil.doubleM
      .verifying("error.too.big", _ <= 100)      // 100 км
      .verifying("error.too.low", _ >= 0.001)    // 1 метров
      .transform [Distance] (
        { km => Distance(km, DistanceUnit.KILOMETERS) },
        { d  => d.distance.toInt }
      )
  }

  /** Тег размещается в области, описываемой кругом. */
  def circleM: Mapping[CircleGs] = {
    mapping(
      centerM,
      "radius"  -> radiusM
    )
    { CircleGs.apply }
    { CircleGs.unapply }
  }

  /** Маппинг формы размещения карточки в тегах. */
  def advFormM = {
    mapping(
      tagsEditFormUtil.existingKm,
      "map"     -> mapStateM,
      "circle"  -> circleM
    )
    { MAdvFormResult.apply }
    { MAdvFormResult.unapply }
  }

  def advForm: GtForm_t = {
    Form(advFormM)
  }

}
