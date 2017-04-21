package util.adv.geo

import com.google.inject.{Inject, Singleton}
import io.suggest.adv.geo.MFormS
import io.suggest.geo.{CircleGs, GeoShape}
import models.adv.geo.cur
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


  /** Рендер выхлопа [[cur.MAdvGeoShapeInfo]] в более простое кросс-платформенной представление.
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


  override val mainValidator = validator[MFormS] { m =>
    m.mapProps is valid
    m.datePeriod is valid
    m.rcvrsMap is valid
    m.tagsEdit is valid
    m.radCircle.each is valid
  }

}
