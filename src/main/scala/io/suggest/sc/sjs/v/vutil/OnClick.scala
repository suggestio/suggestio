package io.suggest.sc.sjs.v.vutil

import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import io.suggest.sjs.common.view.vutil.OnClickT
import org.scalajs.dom.Event

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.06.15 10:23
 * Description: Реализация common-sjs/OnClickT, который отвечает за упрощенное вешанье click-событий.
 */
trait OnClick extends OnClickT {

  override protected def isTouchLocked = MTouchLock()

}


trait OnClickSelfT extends OnClick { that: SafeEventTargetT =>

  protected def onClick[T <: Event](f: T => _): Unit = {
    onClick[T](that)(f)
  }

  protected def _onClickRaw[T <: Event](listener: js.Function1[T, _]): Unit = {
    _onClickRaw[T](that)(listener)
  }

}