package io.suggest.sc.controller.search

import diode._
import io.suggest.sc.model.search._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 15:38
  * Description: Контроллер для общих экшенов поисковой панели.
  */
object SearchAh {

  /** Опциональный экшен инициализации поисковой панели, если данные состояния предрасполагают. */
  def maybeInitSearchPanel(v0: MScSearch): Option[Effect] = {
    var fxAcc = List.empty[Effect]

    // Эффект поиска тегов и узлов вообще. Надо подружать текущие теги/узлы или искать узлы по названию.
    if (v0.geo.found.isEmpty && !v0.geo.found.req.isPending)
      fxAcc ::= reDoSearchFx( ignorePending = false )

    // Эффект инициализации/подгонки карты (разовый).
    if (!v0.geo.mapInit.ready) {
      val mapInitFx = Effect {
        DomQuick
          .timeoutPromiseT(50)(InitSearchMap)
          .fut
      }
      fxAcc ::= mapInitFx
    }

    fxAcc.mergeEffects
  }


  /** Эффект запуска поиска узлов. */
  def reDoSearchFx(ignorePending: Boolean): Effect = {
    DoNodesSearch(clear = true, ignorePending = ignorePending)
      .toEffectPure
  }

}
