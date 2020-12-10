package io.suggest.adn.mapf

import io.suggest.adv.free.MAdv4FreeProps
import io.suggest.bill.MGetPriceResp
import io.suggest.maps.nodes.MRcvrsMapUrlArgs
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 13:47
  * Description: Модель данных для инициализации LamForm. Передаётся с сервера внутри HTML.
  */
object MLamFormInit {

  implicit def lafFormInitJson: OFormat[MLamFormInit] = (
    (__ \ "c").format[MLamConf] and
    (__ \ "p").format[MGetPriceResp] and
    (__ \ "f").format[MLamForm] and
    (__ \ "a").formatNullable[MAdv4FreeProps]
  )(apply, unlift(unapply))

}


/** Класс-контейнер данных инициализации формы.
  *
  * @param priceResp Начальное состояние ценника.
  */
case class MLamFormInit(
                         conf             : MLamConf,
                         priceResp        : MGetPriceResp,
                         form             : MLamForm,
                         adv4FreeProps    : Option[MAdv4FreeProps],
                       )
