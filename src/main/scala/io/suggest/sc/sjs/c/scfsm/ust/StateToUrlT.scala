package io.suggest.sc.sjs.c.scfsm.ust

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.msc.{MScSd, MUrlUtil}
import io.suggest.sc.sjs.vm.SafeWnd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.16 11:40
  * Description: Поддержка связывания состояния ScFsm с URL через History API.
  */
trait StateToUrlT extends ScFsmStub {

  /** Управление состояние закинуто в отдельный сингтон, чисто в целях группировки. */
  object State2Url {

    def currUrlQsEqualsTo(qsStr: String): Boolean = {
      val currQsStr = MUrlUtil.clearUrlHash( _urlHash )
      currQsStr.contains(qsStr)
    }

    /** Заброс текущего состояния FSM в историю. */
    def pushCurrState(): Unit = {
      // Сериализовать куски текущего состояния в URL.
      for (hApi <- SafeWnd.history) {
        val qsStr = MScSd.toQsStr( _stateData )

        // Сверять URL с текущим значением window.location, отличается ли?
        if ( !currUrlQsEqualsTo(qsStr) ) {
          //val n = "\n"
          //println( "pushState: " + System.currentTimeMillis() + " " + url + Thread.currentThread().getStackTrace.iterator.take(5).mkString(n,n,n) )
          hApi.pushState(null, "sio", Some(MUrlUtil.URL_HASH_PREFIX + qsStr))
        }
        //else log("pushCurrState(): Dup state")
      }
    }

  }

}
