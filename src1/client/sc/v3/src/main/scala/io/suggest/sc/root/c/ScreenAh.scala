package io.suggest.sc.root.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.dev.JsScreenUtil
import io.suggest.sc.root.m.{MScScreenS, ScreenReset}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.17 10:46
  * Description: Контроллер, слушающий события экрана устройства.
  */
class ScreenAh[M](modelRW: ModelRW[M, MScScreenS]) extends ActionHandler(modelRW) {

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал изменения размеров/ориентации экрана.
    case ScreenReset =>
      val v0 = value
      val v2 = v0.withScreen(
        screen = JsScreenUtil.getScreen()
      )
      updated(v2)

  }

}
