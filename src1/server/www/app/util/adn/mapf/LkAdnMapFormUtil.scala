package util.adn.mapf

import javax.inject.Inject
import io.suggest.adn.mapf.{AdnMapFormConstants, MLamForm}
import io.suggest.dt.CommonDateTimeUtil
import io.suggest.geo.{CircleGs, MGeoPoint}
import io.suggest.maps.MMapProps
import io.suggest.scalaz.ValidateFormUtilT
import play.api.inject.Injector
import util.adv.AdvFormUtil
import scalaz.ValidationNel
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:25
  * Description: Утиль для формы размещения узла ADN на карте.
  * Пока делаем, чтобы точка была только одна максимум. Это упростит ряд вещей.
  *
  * Будем всячески избегать ситуации в проекте, когда точек узла может быть больше одной.
  */
final class LkAdnMapFormUtil @Inject() (
                                         injector: Injector,
                                       )
  extends ValidateFormUtilT[MLamForm]
{

  private lazy val advFormUtil = injector.instanceOf[AdvFormUtil]


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


  /** Валидация гео-круга под нужды формы .  */
  def adnRadCircleV(gc: CircleGs): ValidationNel[String, CircleGs] = {
    CircleGs.validate(gc, AdnMapFormConstants.Rad.RadiusM)
  }


  /** Основной валидатор для MLamForm. */
  def lamFormV(mf: MLamForm): ValidationNel[String, MLamForm] = {
    (
       advFormUtil.advPeriodV( mf.datePeriod ) |@|
       adnRadCircleV( mf.mapCursor ) |@|
       MMapProps.validate( mf.mapProps ) |@|
       CommonDateTimeUtil.jsDateTzOffsetMinutesV( mf.tzOffsetMinutes )
    ) { (_,_,_,_) => mf }
  }

  override def doValidation(v: MLamForm): ValidationNel[String, MLamForm] = {
    lamFormV(v)
  }

}
