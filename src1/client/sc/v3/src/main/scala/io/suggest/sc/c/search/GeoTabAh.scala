package io.suggest.sc.c.search

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.maps.m.HandleMapReady
import io.suggest.sc.m.search.{InitSearchMap, MGeoTabS}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.18 10:31
  * Description: Контроллер экшенов гео-таба.
  */
class GeoTabAh[M](
                   //api            : IScUniApi,
                   //searchQsRO     : ModelRO[MScQs],
                   modelRW: ModelRW[M, MGeoTabS]
                 )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

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


    // Перехват инстанса leaflet map и сохранение в состояние.
    case m: HandleMapReady =>
      val v0 = value
      val v2 = v0
        .withLmap( Some(m.map) )
      updatedSilent( v2 )

  }

}
