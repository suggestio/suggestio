package io.suggest.sc.c.search

import diode._
import io.suggest.sc.m.search._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 15:38
  * Description: Контроллер для общих экшенов поисковой панели.
  */
object SearchAh {

  /** Вернуть эффект для пере-ресайза гео.карты. */
  def mapResizeFx(lMap: LMap): Effect = {
    Effect {
      // Пауза перед сбросом размера, т.к. карта неправильно определяет свой размер "на лету", только после полного рендера.
      for {
        _ <- DomQuick.timeoutPromise(100).fut
      } yield {
        lMap.invalidateSize(true)
        // TODO Opt надо как-то пустой эффект возвращать без оверхеда.
        DoNothing
      }
    }
  }


  /** Опциональный экшен инициализации поисковой панели, если данные состояния предрасполагают. */
  def maybeInitSearchPanel(v0: MScSearch): Option[Effect] = {
    var fxAcc = List.empty[Effect]

    // Эффект поиска тегов и узлов вообще. Надо подружать текущие теги/узлы или искать узлы по названию.
    if (v0.geo.found.isEmpty && !v0.geo.found.req.isPending)
      fxAcc ::= reDoSearchFx( ignorePending = false )

    // Эффект инициализации/подгонки карты (разовый).
    if (v0.geo.mapInit.ready) {
      // Надо запускать ручной ресайз, иначе карта может неверно увидеть свой фактический размер (т.к. размер окна мог меняться, пока карта была скрыта).
      // TODO Не запускать ресайз, если размер не менялся. У карты крышу сносит, если часто этот метод дёргать.
      for (lInstance <- v0.geo.data.lmap) yield
        SearchAh.mapResizeFx( lInstance )
    } else {
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
