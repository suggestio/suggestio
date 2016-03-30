package util.adv.geo

import com.google.inject.Singleton
import models.adv.geo.IAdvGeoFormResult

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.16 18:12
  * Description: common-утиль для биллинга гео-размещений.
  */
@Singleton
class AdvGeoBillUtil {

  /**
    * Посчитать мультипликатор стоимости на основе даты и радиуса размещения.
    *
    * @param res Результат маппинга формы.
    * @return Double-мультипликатор цены.
    */
  def getPriceMult(res: IAdvGeoFormResult): Double = {
    val daysCount = Math.max(1, res.period.interval.toDuration.getStandardDays) + 1

    // Привести радиус на карте к множителю цены
    val radKm = res.radMapVal.circle.radius.kiloMeters
    val radMult = radKm / 1.5

    radMult * daysCount
  }

}
