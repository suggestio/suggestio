package io.suggest.sc.c.search

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sc.m.ResetUrlRoute
import io.suggest.sc.m.hdr.SearchOpenClose
import io.suggest.sc.m.search._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.spa.DoNothing
import japgolly.univeq._

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
    Effect.action {
      DoNodesSearch(clear = true, ignorePending = ignorePending)
    }
  }

}


class SearchAh[M](
                   modelRW        : ModelRW[M, MScSearch]
                 )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке открытия поисковой панели.
    case m: SearchOpenClose =>
      val v0 = value
      if (v0.isShown ==* m.open) {
        // Ничего делать не надо - ничего не изменилось.
        noChange

      } else {
        // Действительно изменилось состояние отображения панели:
        var v2 = v0.withIsShown( m.open )

        // Аккаумулятор сайд-эффектов.
        val routeFx = Effect.action( ResetUrlRoute )

        // Требуется ли запускать инициализацию карты или списка найденных узлов? Да, если открытие на НЕинициализированной панели.
        val fxOpt = OptionUtil.maybeOpt(m.open) {
          SearchAh.maybeInitSearchPanel(v2)
        }

        // Объеденить эффекты:
        val finalFx = (routeFx :: fxOpt.toList)
          .mergeEffects
          .get

        updated(v2, finalFx)
      }

  }

}
