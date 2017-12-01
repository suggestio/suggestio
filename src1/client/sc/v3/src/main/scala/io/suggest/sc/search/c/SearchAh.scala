package io.suggest.sc.search.c

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.sc.hdr.m.HSearchBtnClick
import io.suggest.sc.search.m._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import japgolly.univeq._
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sjs.common.log.Log

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 15:38
  * Description: Контроллер для общих экшенов поисковой панели.
  */
class SearchAh[M](
                   modelRW        : ModelRW[M, MScSearch]
                 )
  extends ActionHandler( modelRW )
  with Log
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке открытия поисковой панели.
    case HSearchBtnClick =>
      val v2 = value.withIsShown( true )

      if (v2.mapInit.ready) {
        updated( v2 )
      } else {
        val fx = Effect {
          DomQuick
            .timeoutPromiseT(25)(InitSearchMap)
            .fut
        }
        updated( v2, fx )
      }


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

        // TODO Если на панель тегов, то надо запустить эффект поиска текущих (начальных) тегов.
        if ((m.newTab ==* MSearchTabs.Tags) && v2.tags.tagsReq.isEmpty) {
          // Переключение на неинициализированную панель тегов: запустить загрузку тегов.
          val fx = Effect.action {
            GetMoreTags(clear = true)
          }
          updated(v2, fx)
        } else {
          // Инициализация тегов не требуется.
          updated(v2)
        }
      }


    // Команда сброса текущего состояния тегов.
    case ResetTags =>
      val v0 = value

      // Требуется ли сразу перезагружать список тегов? Да, если открыта search-панель и вкладка тегов -- текущая.
      val needFxOpt = OptionUtil.maybe( v0.isShown && v0.currTab ==* MSearchTabs.Tags ) {
        Effect.action( GetMoreTags(clear = true) )
      }

      val emptyState = MTagsSearchS.empty
      val v2Opt = OptionUtil.maybe( v0.tags !=* emptyState ) {
        v0.withTags(emptyState)
      }

      this.optionalResult( v2Opt, needFxOpt )

  }

}
