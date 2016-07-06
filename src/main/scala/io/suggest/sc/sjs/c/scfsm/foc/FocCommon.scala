package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.m.msrv.foc.find.MFocAds
import io.suggest.sc.sjs.vm.res.FocusedRes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.07.16 14:11
  * Description: Общая утиль для focused-выдачи.
  */
object FocCommon {

  /** Залить focused-стили в DOM. */
  def appendStyles(resp: MFocAds): Unit = {
    for (styles <- resp.styles; res <- FocusedRes.find()) {
      res.appendCss(styles)
    }
  }

}
