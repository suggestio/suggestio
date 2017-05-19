package io.suggest.lk.adn.map.a

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.adn.map.m.HandleAdvGeoExistPopupResp
import io.suggest.lk.adn.map.u.ILkAdnMapApi
import io.suggest.maps.m.{HandleMapPopupClose, MExistGeoPopupS, OpenAdvGeoExistPopup}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.05.17 14:36
  * Description: Контроллер для попапа текущего гео-размещения.
  */
class CurrentGeoPopupAh[M](
                         api      : ILkAdnMapApi,
                         modelRW  : ModelRW[M, MExistGeoPopupS]
                       )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал к открытию попапа над указанным шейпом.
    case m: OpenAdvGeoExistPopup =>
      val fx = Effect {
        api
          .currentGeoItemPopup( m.itemId )
          .transform { tryResp =>
            val r = HandleAdvGeoExistPopupResp(m, tryResp)
            Success(r)
          }
      }
      val v0 = value
      val v2 = value.copy(
        content = v0.content.pending(),
        state   = Some(m)
      )
      updated(v2, fx)


    // Сигнал с результом запроса к серверу
    case m: HandleAdvGeoExistPopupResp =>
      // Проверить, актуален ли данный ответ сервера, для текущего ли он item'а.
      val v0 = value
      // TODO Opt contains хватило бы eq сравнения вместо ==.
      if (v0.state.contains(m.open)) {
        // Это ожидаемый результат запроса. Залить его в состояние...
        val content2 = m.tryResp.fold(
          v0.content.fail,
          v0.content.ready
        )
        val v2 = v0.withContent( content2 )
        updated(v2)

      } else {
        // Неактуальный ответ.
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m.open )
        noChange
      }

    // Реакция на сигнал закрытия попапа, когда он открыт.
    case HandleMapPopupClose if value.state.nonEmpty =>
      val v2 = value.copy(
        content   = Pot.empty,
        state     = None
      )
      updated(v2)

  }

}
