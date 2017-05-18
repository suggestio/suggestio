package util.adv.geo

import com.google.inject.{Inject, Singleton}
import io.suggest.adv.geo.{AdvGeoConstants, MFormS, MMapProps, RcvrsMap_t}
import io.suggest.geo.{CircleGs, GeoShape, MGeoCircle, MGeoPoint}
import models.adv.geo.cur._
import play.extras.geojson.{Feature, LatLng}
import util.data.{AccordUtil, AccordValidateFormUtilT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 21:45
 * Description: Утиль для формы размещения карточки в геотегах.
 */
@Singleton
class AdvGeoFormUtil @Inject() (
                                 accordUtil        : AccordUtil
                               )
  extends AccordValidateFormUtilT[MFormS]
{


  /**
    * Для adv-форм с radmap-картой требуется начальное состояние карты.
    * Тут -- сборка состояния на основе начальной точки.
    *
    * @param gp Начальная точка карты. Будет в центре выделенного круга.
    * @return MMapProps.
    */
  def mapProps0(gp: MGeoPoint): MMapProps = {
    MMapProps(gp, zoom = 13)
  }
  /**
    * Для форм с radmap-картой требуется начальное состояние rad-компонента.
    * Тут -- сборка начального состояния гео-круга на основе начальной точки.
    *
    * @param gp Начальная точка карты. Будет в центре выделенного круга.
    * @return MGeoCircle.
    */
  def radCircle0(gp: MGeoPoint): MGeoCircle = {
    MGeoCircle(
      center  = gp,
      radiusM = 500
    )
  }


  /** Рендер выхлопа [[models.adv.geo.cur.MAdvGeoShapeInfo]] в более простое кросс-платформенной представление.
    * Этот костыль связан с тем, что GeoShape не является кросс-платформенной моделью, а сырой GeoJSON пропихнуть
    * Это во многом аналогично обычному shapeItems2geoJson, но более лениво в плане рендера попапа:
    * js должен обращаться к серверу за попапом. Поэтому, это легковеснее, быстрее, и Context здесь не нужен.
    */
  def shapeInfo2geoJson(si: MAdvGeoShapeInfo): Feature[LatLng] = {
    val gs = GeoShape.parse(si.geoShapeStr)
    val props = GjFtProps(
      itemId      = si.itemId,
      // hasApproved влияет на цвет заливки.
      hasApproved = si.hasApproved,
      crclRadiusM = gs match {
        case crcl: CircleGs => Some(crcl.radius.meters)
        case _              => None
      }
    )
    Feature(
      // Если circle, то будет отрендерена точка. Поэтому радиус задан в props.
      geometry    = gs.toPlayGeoJsonGeom,
      // Собрать пропертисы для этой feature:
      properties  = Some( GjFtProps.FORMAT.writes(props) )
    )
  }


  import com.wix.accord.dsl._
  import accordUtil._


  implicit val rcvrsMapV = validator[RcvrsMap_t] { rm =>
    rm.size should be <= AdvGeoConstants.AdnNodes.MAX_RCVRS_PER_TIME
    rm.keys.each is valid
  }

  implicit val geoCircleV = validator[MGeoCircle] { gc =>
    gc.center is valid
    gc.radiusM should be >= AdvGeoConstants.Radius.MIN_M.toDouble
    gc.radiusM should be <= AdvGeoConstants.Radius.MAX_M.toDouble
  }


  override val mainValidator = validator[MFormS] { m =>
    m.mapProps is valid
    m.datePeriod is valid
    m.rcvrsMap is valid
    m.tagsEdit is valid
    m.radCircle.each is valid
  }

}
