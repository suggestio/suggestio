package util.adn.mapf

import com.google.inject.Inject
import io.suggest.adn.mapf.MLamForm
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

  import com.wix.accord.dsl._
  import accordUtil._

  /** Основной валидатор для MLamForm. */
  // TODO Кривой wix.accord глючит, если написать implicit val тут. Но оно implicit в трейте, поэтому пока терпим.
  override val mainValidator = validator[MLamForm] { mf =>
    mf.datePeriod is valid
    mf.coord is valid
    mf.mapProps is valid
    mf.tzOffsetMinutes is valid( jsDateTzOffsetMinutesV )
  }

}
