package io.suggest.sc.c.search

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.maps.m.HandleMapReady
import io.suggest.msg.ErrorMsgs
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sc.m.ResetUrlRoute
import io.suggest.sc.m.hdr.SearchOpenClose
import io.suggest.sc.m.search._
import io.suggest.sc.search.{MSearchTab, MSearchTabs}
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
    Effect.action {
      //println("invalidate size")
      lMap.invalidateSize(true)
      DoNothing
    }
  }

}


class SearchAh[M](
                   modelRW        : ModelRW[M, MScSearch]
                 )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  private def _maybeInitializeTab(tab: MSearchTab, v0: MScSearch): Option[Effect] = {
    tab match {
      case MSearchTabs.Tags if v0.tags.tagsReq.isEmpty =>
        // Неинициализированная панель тегов: запустить загрузку тегов.
        val getMoreTagsFx = Effect.action {
          GetMoreTags(clear = true)
        }
        Some(getMoreTagsFx)

      case MSearchTabs.GeoMap =>
        if (v0.mapInit.ready) {
          // Надо запускать ручной ресайз, иначе карта может неверно увидеть свой фактический размер (т.к. размер окна мог меняться, пока карта была скрыта).
          // TODO Не запускать ресайз, если размер не менялся. У карты крышу сносит, если часто этот метод дёргать.
          for (lInstance <- v0.mapInit.lmap) yield {
            SearchAh.mapResizeFx( lInstance )
          }
        } else {
          val mapInitFx = Effect {
            DomQuick
              .timeoutPromiseT(25)(InitSearchMap)
              .fut
          }
          Some( mapInitFx )
        }

      case _ =>
        None
    }
  }

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке открытия поисковой панели.
    case m: SearchOpenClose =>
      var v2 = value.withIsShown( m.open )

      // Если выставлен таб, то залить его в состояние:
      for (tab <- m.onTab if v2.currTab !=* tab)
        v2 = v2.withCurrTab(tab)

      // Аккаумулятор сайд-эффектов.
      val routeFx = Effect.action( ResetUrlRoute )

      // Требуется ли запусткать инициализацию карты или списка тегов? Да, если открытие на НЕинициализированной панели.
      val fxOpt = OptionUtil.maybeOpt(m.open)( _maybeInitializeTab(m.onTab.getOrElse(v2.currTab), v2) )

      // Объеденить эффекты:
      val finalFx = (routeFx :: fxOpt.toList)
        .mergeEffectsSet
        .get

      updated(v2, finalFx)


    // Запуск инициализации гео.карты.
    case InitSearchMap =>
      // Сбросить флаг инициализации карты, чтобы гео.карта тоже отрендерилась на экране.
      val v0 = value

      if (!v0.mapInit.ready) {
        val v2 = v0.withMapInit(
          v0.mapInit
            .withReady(true)
        )
        updated( v2 )

      } else {
        noChange
      }


    // Смена текущего таба на панели поиска.
    case m: SwitchTab =>
      val v0 = value
      if (v0.currTab ==* m.newTab) {
        noChange

      } else {
        // Смена текущей вкладки.
        val v2 = v0.withCurrTab( m.newTab )

        var fx: Effect = Effect.action( ResetUrlRoute )

        // Если таб ещё не инициализирован, то сделать это.
        for (tabInitFx <- _maybeInitializeTab(m.newTab, v2))
          fx += tabInitFx

        updated(v2, fx)
      }


    // Команда сброса текущего состояния тегов.
    case ResetTags =>
      val v0 = value

      val tagsVisible = v0.isShownTab( MSearchTabs.Tags )
      // Требуется ли сразу перезагружать список тегов? Да, если открыта search-панель и вкладка тегов -- текущая.
      val needFxOpt = OptionUtil.maybe( tagsVisible ) {
        Effect.action( GetMoreTags(clear = true) )
      }

      val emptyState = MTagsSearchS.empty
      val v2Opt = OptionUtil.maybe( v0.tags !=* emptyState ) {
        v0.withTags(emptyState)
      }

      // Использовать silent update, если вкладка тегов не видна на экране.
      if (tagsVisible)
        this.optionalResult( v2Opt, needFxOpt )
      else
        this.optionalSilentResult(v2Opt, needFxOpt)


    // Принудительный запуск поиска на текущей вкладке.
    case m @ ReDoSearch =>
      val v0 = value
      v0.currTab match {
        case MSearchTabs.Tags =>
          val fx = Effect.action {
            GetMoreTags(clear = true, ignorePending = true)
          }
          effectOnly(fx)

        case t @ MSearchTabs.GeoMap =>
          LOG.error( ErrorMsgs.NOT_IMPLEMENTED, msg = (m, t) )
          noChange
      }

    // Перехват инстанса leaflet map и сохранение в состояние.
    case m: HandleMapReady =>
      val v0 = value
      val v2 = v0.withMapInit(
        v0.mapInit.withLInstance( Some(m.map) )
      )
      updatedSilent( v2 )

  }

}
