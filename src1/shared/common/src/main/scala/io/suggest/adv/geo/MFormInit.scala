package io.suggest.adv.geo

import boopickle.Default._
import io.suggest.adv.free.MAdv4FreeProps
import io.suggest.bill.MGetPriceResp

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

  implicit val pickler: Pickler[MFormInit] = {
    implicit val a4fP   = MAdv4FreeProps.pickler
    implicit val formP  = MFormS.pickler
    generatePickler[MFormInit]
  }

}

case class MFormInit(
  adId          : String,
  adv4FreeProps : Option[MAdv4FreeProps],
  advPricing    : MGetPriceResp,
  form          : MFormS
)
