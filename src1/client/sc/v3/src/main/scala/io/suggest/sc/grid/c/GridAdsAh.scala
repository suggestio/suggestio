package io.suggest.sc.grid.c

import diode.{ActionHandler, ActionResult, ModelRO, ModelRW}
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sc.grid.m.MGridS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 18:59
  * Description: Контроллер плитки карточек.
  *
  * @param searchArgsRO Доступ к аргументам поиска карточек.
  */
class GridAdsAh[M](
                    api             : IFindAdsApi,
                    searchArgsRO    : ModelRO[MFindAdsReq],
                    modelRW         : ModelRW[M, MGridS]
                  )
  extends ActionHandler(modelRW)
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    case _ =>
      ???

  }

}
