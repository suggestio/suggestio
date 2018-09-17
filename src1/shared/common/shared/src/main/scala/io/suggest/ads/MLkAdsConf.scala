package io.suggest.ads

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.jd.MJdConf
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:25
  * Description: Form-данные LkAds, для обмена общими данными формы между клиентом и сервером.
  */
object MLkAdsConf {

  implicit def univEq: UnivEq[MLkAdsConf] = UnivEq.derive

  implicit def MLK_ADS_FORM_FORMAT: OFormat[MLkAdsConf] = (
    (__ \ "i").format[RcvrKey] and
    (__ \ "c").format[MJdConf]
  )(apply, unlift(unapply))

}


/** Контейнер данных формы LkAds.
  *
  * @param nodeKey путь до узла.
  */
case class MLkAdsConf(
                       nodeKey  : RcvrKey,
                       jdConf   : MJdConf
                     )
