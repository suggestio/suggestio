package io.suggest.ads

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 18:37
  * Description: Модель данных формы управления карточками.
  */
object MLkAdsFormInit {

  /** Поддержка play-json. */
  implicit def MADS_FORM_INIT_FORMAT: OFormat[MLkAdsFormInit] = {
    (__ \ "f")
      .format[MLkAdsForm]
      .inmap[MLkAdsFormInit]( apply, _.form )
  }

  implicit def univEq: UnivEq[MLkAdsFormInit] = UnivEq.derive

}


/** Контейнер данных для инициализации формы управления карточками узла.
  *
  * @param form Данные обмена формы.
  */
case class MLkAdsFormInit(
                           form: MLkAdsForm
                         )
