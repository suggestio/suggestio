package io.suggest.adv.geo

import boopickle.Default._
import io.suggest.adv.free.MAdv4FreeProps
import io.suggest.bill.MGetPriceResp
import io.suggest.maps.nodes.MRcvrsMapUrlArgs

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
    implicit val a4fP   = MAdv4FreeProps.a4fPropsPickler
    implicit val advPricingP = MGetPriceResp.getPriceRespPickler
    implicit val formP  = MFormS.pickler
    implicit val rcvrsMapUrlArgsP = MRcvrsMapUrlArgs.rcvrsMapUrlArgsP
    generatePickler[MFormInit]
  }

}

case class MFormInit(
                      adId          : String,
                      adv4FreeProps : Option[MAdv4FreeProps],
                      advPricing    : MGetPriceResp,
                      form          : MFormS,
                      rcvrsMap      : MRcvrsMapUrlArgs,
                    )