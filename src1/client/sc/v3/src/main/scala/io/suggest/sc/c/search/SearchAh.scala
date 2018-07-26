package io.suggest.sc.c.search

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.spa.DiodeUtil.Implicits._
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
          DoTagsSearch(clear = true)
        }
        Some(getMoreTagsFx)

      case MSearchTabs.GeoMap =>
        if (v0.geo.mapInit.ready) {
          // Надо запускать ручной ресайз, иначе карта может неверно увидеть свой фактический размер (т.к. размер окна мог меняться, пока карта была скрыта).
          // TODO Не запускать ресайз, если размер не менялся. У карты крышу сносит, если часто этот метод дёргать.
          for (lInstance <- v0.geo.data.lmap) yield {
            SearchAh.mapResizeFx( lInstance )
          }
        } else {
          val mapInitFx = Effect {
            DomQuick
              .timeoutPromiseT(50)(InitSearchMap)
              .fut
          }
          Some( mapInitFx )
        }

      case _ =>
        None
    }
  }

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

        // Если выставлен таб, то залить его в состояние:
        for (tab <- m.onTab if v2.currTab !=* tab)
          v2 = v2.withCurrTab(tab)

        // Аккаумулятор сайд-эффектов.
        val routeFx = Effect.action( ResetUrlRoute )

        // Требуется ли запускать инициализацию карты или списка тегов? Да, если открытие на НЕинициализированной панели.
        val fxOpt = OptionUtil.maybeOpt(m.open) {
          val nextTab = m.onTab getOrElse v2.currTab
          _maybeInitializeTab(nextTab, v2)
        }

        // Объеденить эффекты:
        val finalFx = (routeFx :: fxOpt.toList)
          .mergeEffectsSet
          .get

        updated(v2, finalFx)
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

        // Если изменился поиск, а новый таб не искался в таком запросе, то надо запустить поиск в новом табе:
        val tgTextQuery = v0.text.searchQuery.toOption
        val tgPot = v0.tabSearchPot( m.newTab )
        val isNeedTextChangedFx = tgPot.exists(_.textQuery !=* tgTextQuery)
        if (isNeedTextChangedFx)
          fx += _reDoSearchFx(m.newTab)

        updated(v2, fx)
      }


    // Команда сброса текущего состояния тегов.
    case ResetTags =>
      val v0 = value

      val tagsVisible = v0.isShownTab( MSearchTabs.Tags )
      // Требуется ли сразу перезагружать список тегов? Да, если открыта search-панель и вкладка тегов -- текущая.
      val needFxOpt = OptionUtil.maybe( tagsVisible ) {
        Effect.action( DoTagsSearch(clear = true) )
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
    case ReDoSearch =>
      val v0 = value
      val fx = _reDoSearchFx(v0.currTab)
      effectOnly(fx)

  }


  def _reDoSearchFx(currTab: MSearchTab): Effect = {
    Effect.action {
      currTab match {
        case MSearchTabs.Tags =>
          DoTagsSearch(clear = true, ignorePending = true)
        case MSearchTabs.GeoMap =>
          DoGeoSearch(clear = true)
      }
    }
  }

}
