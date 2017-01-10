package io.suggest.adv.geo

import boopickle.Default._

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
    implicit val a4fP = MAdv4FreeProps.pickler
    generatePickler[MFormInit]
  }

}

case class MFormInit(
  adId          : String,
  adv4FreeProps : MAdv4FreeProps
  // TODO Начальные данные формы - сюда же отдельным полем.
)
