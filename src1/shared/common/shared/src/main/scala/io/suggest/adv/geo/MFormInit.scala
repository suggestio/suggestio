package io.suggest.adv.geo

import io.suggest.adv.free.MAdv4FreeProps
import io.suggest.bill.MGetPriceResp
import io.suggest.maps.nodes.MRcvrsMapUrlArgs
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 18:11
  * Description: Данные с сервера для начальной инициализации формы.
  *
  * В эту форму скидываются сериализуемые данные, которые сервер хочет донести до js-клиента
  * на первой стадии инициализации JS-формы георазмещения.
  */
object MFormInit {

  implicit def advGeoFormInitJson: OFormat[MFormInit] = (
    (__ \ "i").format[String] and
    (__ \ "e").formatNullable[MAdv4FreeProps] and
    (__ \ "p").format[MGetPriceResp] and
    (__ \ "f").format[MFormS] and
    (__ \ "r").format[MRcvrsMapUrlArgs] and
    (__ \ "d").format[Boolean]
  )(apply, unlift(unapply))

}

case class MFormInit(
                      adId          : String,
                      adv4FreeProps : Option[MAdv4FreeProps],
                      advPricing    : MGetPriceResp,
                      form          : MFormS,
                      rcvrsMap      : MRcvrsMapUrlArgs,
                      radEnabled    : Boolean,
                    )
