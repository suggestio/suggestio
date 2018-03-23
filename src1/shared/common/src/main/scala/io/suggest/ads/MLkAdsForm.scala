package io.suggest.ads

import io.suggest.adv.rcvr.RcvrKey
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:25
  * Description: Form-данные LkAds, для обмена общими данными формы между клиентом и сервером.
  */
object MLkAdsForm {

  implicit def univEq: UnivEq[MLkAdsForm] = UnivEq.derive

  implicit def MLK_ADS_FORM_FORMAT: OFormat[MLkAdsForm] = {
    (__ \ "i")
      .format[RcvrKey]
      .inmap[MLkAdsForm]( apply, _.nodeKey )
  }

}


/** Контейнер данных формы LkAds.
  *
  * @param nodeKey путь до узла.
  */
case class MLkAdsForm(
                       nodeKey: RcvrKey
                     )
