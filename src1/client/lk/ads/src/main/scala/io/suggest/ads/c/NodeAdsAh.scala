package io.suggest.ads.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ads.a.ILkAdsApi
import io.suggest.ads.m.{GetMoreAds, MCurrNodeS}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 21:44
  * Description: Контроллер карточек.
  */
class NodeAdsAh[M](
                    api       : ILkAdsApi,
                    modelRW   : ModelRW[M, MCurrNodeS]
                  )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда к скачке карточек с сервера.
    case m: GetMoreAds =>
      ???

  }

}
