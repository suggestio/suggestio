package util.maps

import com.google.inject.{Inject, Singleton}
import io.suggest.model.geo.{CircleGs, Distance}
import models.maps.RadMapValue
import org.elasticsearch.common.unit.DistanceUnit
import io.suggest.common.maps.MapFormConstants.STATE_FN
import io.suggest.common.maps.rad.RadMapConstants._
import play.api.data.Forms._
import play.api.data.Mapping
import util.FormUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.16 16:37
  * Description: Утиль для форм, завязанных на rad-map, т.е. карту с заданием радиуса.
  */
@Singleton
class RadMapFormUtil @Inject() (
  mapFormUtil: MapFormUtil
) {

  // TODO В ожидании DI.
  private def formUtil = FormUtil


  // TODO Разрешить нецелый километраж?
  def radiusM: Mapping[Distance] = {
    formUtil.doubleM
      .verifying("error.too.big", _ <= 200000)
      // Запретить ставить слишком малый радиус, чтобы цена не была нулевая.
      .verifying("error.too.low", _ >= 10)
      .transform [Distance] (
        { m  => Distance(m, DistanceUnit.METERS) },
        { d  => d.distance.toInt }    // TODO Отрабатывать другие единицы измерения.
      )
  }

  /** Тег размещается в области, описываемой кругом. */
  def circleM: Mapping[CircleGs] = {
    mapping(
      mapFormUtil.centerKM,
      RADIUS_FN  -> radiusM
    )
    { CircleGs.apply }
    { CircleGs.unapply }
  }

  /** Общий состояния карты и её полезной нагрузки. */
  def radMapValM: Mapping[RadMapValue] = {
    mapping(
      STATE_FN   -> mapFormUtil.mapStateM,
      CIRCLE_FN  -> circleM
    )
    { RadMapValue.apply }
    { RadMapValue.unapply }
  }

}
