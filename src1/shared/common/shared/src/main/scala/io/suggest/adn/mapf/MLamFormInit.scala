package io.suggest.adn.mapf

import io.suggest.adv.free.MAdv4FreeProps
import io.suggest.bill.MGetPriceResp
import io.suggest.maps.nodes.MRcvrsMapUrlArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 13:47
  * Description: Модель данных для инициализации LamForm. Передаётся с сервера внутри HTML.
  */
object MLamFormInit {

  import boopickle.Default._

  /** Поддержка бинарной сериализации. */
  implicit def mLamFormInitPickler: Pickler[MLamFormInit] = {
    implicit val mGetPriceRespP = MGetPriceResp.getPriceRespPickler
    implicit val mLamFormP = MLamForm.mLamFormPickler
    implicit val a4fPropsP = MAdv4FreeProps.a4fPropsPickler
    implicit val rcvrsMapUrlArgsP = MRcvrsMapUrlArgs.rcvrsMapUrlArgsP
    generatePickler[MLamFormInit]
  }

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
