package io.suggest.lk.adn.map.a

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.adn.mapf.opts.MLamOpts
import io.suggest.lk.adn.map.m.{OnAdvsMapChanged, OnGeoLocChanged}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.17 16:38
  * Description: Контроллер опций (галочек) LAM-формы.
  */
class LamOptsAh[M](
                    modelRW           : ModelRW[M, MLamOpts],
                    priceUpdateFx     : Effect
                  )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case oglc: OnGeoLocChanged =>
      val v2 = value.withOnGeoLoc( oglc.checked )
      updated(v2, priceUpdateFx)

    case oamc: OnAdvsMapChanged =>
      val v2 = value.withOnAdvMap( oamc.checked )
      updated(v2, priceUpdateFx)

  }

}
