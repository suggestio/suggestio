package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m.layout.MSlideBlocks
import io.suggest.lk.m.SlideBlockClick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.17 12:24
  * Description: Контроллер управления slide-блоками редактора.
  */
class SlideBlocksAh[M](modelRW: ModelRW[M, MSlideBlocks]) extends ActionHandler(modelRW) {

  override val handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по заголовку какого-то слайд-блока.
    case m: SlideBlockClick =>
      val v0 = value
      val expanded2 = if (v0.expanded contains m.key) {
        None
      } else {
        Some( m.key )
      }
      val v2 = v0.withExpanded( expanded2 )
      updated(v2)

  }

}
