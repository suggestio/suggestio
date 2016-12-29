package io.suggest.lk.adv.geo.a.geo.adv

import diode._
import diode.data.Pot
import io.suggest.lk.adv.geo.a.{CurrGeoAdvsInit, SetCurrGeoAdvs}
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.geo.json.GjFeature

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 17:23
  * Description: Обработчик сигнал (пере)инициализации кружочков текущего размещения.
  */
class GeoAdvExistInitAh[M](
                          api         : ILkAdvGeoApi,
                          adIdProxy   : ModelRO[String],
                          existAdvsRW : ModelRW[M, Pot[js.Array[GjFeature]]]
                        )
  extends ActionHandler(existAdvsRW)
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
      // Надо бы отфильтровать null'ы, т.к. сервер генерит chunked с "особенностями" из-за проблем с овладеванием akka-streams Source.
      val resp2 = resp.filter(_ != null)
      updated(  value.ready(resp2) )

  }

}
