package io.suggest.lk.adn.map.a

import diode._
import diode.data.Pot
import io.suggest.geo.json.GjFeature
import io.suggest.lk.adn.map.m.{CurrGeoAdvsInit, SetCurrGeoAdvs}
import io.suggest.lk.adn.map.u.ILkAdnMapApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._

import scala.scalajs.js
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.17 15:29
  * Description: Одноразовый контроллер для инициализации карты текущих размещений узла на карте.
  */
class CurrentGeoAh[M](
                       api          : ILkAdnMapApi,
                       modelRW      : ModelRW[M, Pot[js.Array[GjFeature]]],
                       nodeIdProxy  : ModelRO[String]
                     )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал к инициализации карты текущих размещений узла.
    case CurrGeoAdvsInit =>
      val fx = Effect {
        api
          .currentNodeGeoGj(
            nodeId = nodeIdProxy()
          )
          .transform { tryResp =>
            val r = SetCurrGeoAdvs( tryResp )
            Success( r )
          }
      }
      updated( value.pending(), fx )


    // Сигнал с результатом запроса к серверу за текущими размещениями узла.
    case scga: SetCurrGeoAdvs =>
      // Надо бы отфильтровать null'ы, т.к. сервер генерит chunked с "особенностями" из-за проблем с овладеванием akka-streams Source.
      val v2 = value.withTry( scga.tryResp )
      updated(v2)

  }

}
