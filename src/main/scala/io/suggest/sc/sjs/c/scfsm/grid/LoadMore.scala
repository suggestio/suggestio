package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.m.msc.MFindAdsArgsLimOff
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.08.15 18:01
 * Description: Аддон для сборки grid load more состояний.
 */
trait LoadMore extends OnGrid with Append {

  /** Происходит подгрузка карточек в плитку. */
  trait OnGridLoadingMoreStateT extends GridBlockClickStateT with GridAdsWaitLoadStateT {

    override def afterBecome(): Unit = {
      super.afterBecome()

      // Запустить подгрузку ещё-карточек
      val args = MFindAdsArgsLimOff(_stateData)
      val fut = MFindAds.findAds( args )
      _sendFutResBack(fut)
    }

  }

}
