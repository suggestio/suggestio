package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.vm.foc.FRoot
import io.suggest.sc.sjs.vm.res.FocusedRes
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.vm.content.ClearT
import io.suggest.sc.ScConstants.Focused.SLIDE_ANIMATE_MS
import io.suggest.sc.sjs.m.msrv.foc.MScRespAdsFoc
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.07.16 14:11
  * Description: Общая утиль для focused-выдачи.
  */
object FocCommon {

  /** Залить focused-стили в DOM. */
  def appendStyles(resp: MScRespAdsFoc): Unit = {
    for (styles <- resp.styles; res <- FocusedRes.find()) {
      res.appendCss(styles)
    }
  }


  def closeFocused(fRootOpt: Option[FRoot] = FRoot.find()) {
    for (froot <- fRootOpt) {
      // В фоне запланировать сокрытие focused-выдачи и прочие асинхронные действия.
      Future {
        froot.disappearTransition()
        DomQuick.setTimeout(SLIDE_ANIMATE_MS) { () =>
          clearFocused(fRootOpt)
        }
      }
    }
  }

  def clearFocused(fRootOpt: Option[FRoot] = FRoot.find()): Unit = {
    for (fRoot <- fRootOpt) {
      fRoot.reset()
    }
    FocusedRes.find()
      .foreach(ClearT.f)
  }

}
