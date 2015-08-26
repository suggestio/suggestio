package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.c.scfsm.FindAdsUtil
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.08.15 18:01
 * Description: Аддон для сборки grid load more состояний.
 */
trait LoadMore extends OnGrid with Append {

  /** Происходит подгрузка карточек в плитку. */
  trait OnGridLoadingMoreStateT extends GridBlockClickStateT with FindAdsUtil with GridAdsWaitLoadStateT {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val fut = _findAds( _stateData )
      _sendFutResBack(fut)
    }

  }

}
