package io.suggest.ads

import io.suggest.adv.rcvr.RcvrKey
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
    (__ \ "i")
      .format[RcvrKey]
      .inmap[MLkAdsFormInit]( apply, _.nodeKey )
  }

  @inline implicit def univEq: UnivEq[MLkAdsFormInit] = UnivEq.derive

}


/** Контейнер данных для инициализации формы управления карточками узла.
  *
  * @param nodeKey Ключ узла.
  */
case class MLkAdsFormInit(
                           nodeKey: RcvrKey
                         )
