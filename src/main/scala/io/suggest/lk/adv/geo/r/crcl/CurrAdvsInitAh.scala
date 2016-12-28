package io.suggest.lk.adv.geo.r.crcl

import diode._
import diode.data.Pot
import io.suggest.lk.adv.geo.a.{CurrGeoAdvsInit, SetCurrGeoAdvs}
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 17:23
  * Description: Обработчик сигнал (пере)инициализации кружочков текущего размещения.
  */
class CurrAdvsInitAh[M](
                         api: ILkAdvGeoApi,
                         adIdProxy: ModelRO[String],
                         modelRW: ModelRW[M, Pot[js.Array[GjFeature]]]
                       )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал к инициализации карты кружочков текущий размещений.
    case CurrGeoAdvsInit =>
      val fx = Effect {
        for (resp <- api.currAdvsMap(adId = adIdProxy())) yield {
          SetCurrGeoAdvs(resp)
        }
      }
      updated( value.pending(), fx )

    // Получен ответ сервера на тему текущих размещений.
    case SetCurrGeoAdvs(resp) =>
      updated(  value.ready(resp) )

  }

}
