package io.suggest.sc.search.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.sc.hdr.m.HSearchBtnClick
import io.suggest.sc.search.m.{InitSearchMap, MScSearch, SwitchTab}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 15:38
  * Description: Контроллер для общих экшенов поисковой панели.
  */
class SearchAh[M](
                  modelRW: ModelRW[M, MScSearch]
                 )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке открытия поисковой панели.
    case HSearchBtnClick =>
      val v2 = value.withIsShown( true )

      if (v2.mapInit.ready) {
        updated( v2 )
      } else {
        val fx = Effect {
          DomQuick
            .timeoutPromiseT(15)(InitSearchMap)
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
      val v2 = value.withCurrTab( m.newTab )
      updated( v2 )

  }

}
