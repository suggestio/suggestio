package util.adn.mapf

import com.google.inject.Inject
import io.suggest.adn.mapf.{AdnMapFormConstants, MLamForm}
import io.suggest.adv.geo.MMapProps
import io.suggest.geo.{CircleGs, MGeoPoint}
import util.data.{AccordUtil, AccordValidateFormUtilT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:25
  * Description: Утиль для формы размещения узла ADN на карте.
  * Пока делаем, чтобы точка была только одна максимум. Это упростит ряд вещей.
  *
  * Будем всячески избегать ситуации в проекте, когда точек узла может быть больше одной.
  */
class LkAdnMapFormUtil @Inject() (
                                   accordUtil        : AccordUtil
                                 )
  extends AccordValidateFormUtilT[MLamForm]
{

  def mapProps0(gp0: MGeoPoint): MMapProps = {
    MMapProps(
      center  = gp0,
      zoom    = AdnMapFormConstants.MAP_ZOOM_DFLT
    )
  }

  def radCircle0(gp0: MGeoPoint): CircleGs = {
    CircleGs(
      center  = gp0,
      radiusM = AdnMapFormConstants.Rad.RadiusM.DEFAULT
    )
  }


  import accordUtil._
  import com.wix.accord.dsl._

  implicit val geoCircleV = validator[CircleGs] { gc =>
    gc.center is valid
    gc.radiusM should be >= AdnMapFormConstants.Rad.RadiusM.MIN_M.toDouble
    gc.radiusM should be <= AdnMapFormConstants.Rad.RadiusM.MAX_M.toDouble
  }

  /** Основной валидатор для MLamForm. */
  // TODO Кривой wix.accord глючит, если написать implicit val тут. Но оно implicit в трейте, поэтому пока терпим.
  override val mainValidator = validator[MLamForm] { mf =>
    mf.datePeriod is valid
    mf.mapCursor is valid
    mf.mapProps is valid
    mf.tzOffsetMinutes is valid( jsDateTzOffsetMinutesV )
  }

}
