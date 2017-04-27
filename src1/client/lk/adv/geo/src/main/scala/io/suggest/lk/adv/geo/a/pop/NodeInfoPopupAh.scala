package io.suggest.lk.adv.geo.a.pop

import diode.data.Pending
import diode._
import io.suggest.lk.adv.geo.m.{MNodeInfoPopupS, MOther, OpenNodeInfoClick, OpenNodeInfoResp}
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.lk.m.ILkCommonPopupCloseAction
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 17:44
  * Description: Контроллер по чтению инфы по узлам.
  */
class NodeInfoPopupAh[M](
                          api      : ILkAdvGeoApi,
                          confRO   : ModelRO[MOther],
                          modelRW  : ModelRW[M, Option[MNodeInfoPopupS]]
                        )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал открытия узла.
    case m: OpenNodeInfoClick =>
      val rcvrKey = m.rcvrKey
      val nodeId = rcvrKey.last

      // Организовать запрос к серверу.
      val fx = Effect {
        api
          .nodeAdvInfo(nodeId, adId = confRO().adId)
          .transform { tryRes =>
            val r = OpenNodeInfoResp(rcvrKey, tryRes)
            Success(r)
          }
      }

      // Выставить в состояние данные по идущему запросу.
      val v2 = Some(
        MNodeInfoPopupS(
          rcvrKey = rcvrKey,
          req     = Pending()
        )
      )

      updated(v2, fx)


    // Сигнал открытия попапа для узла.
    case m: OpenNodeInfoResp =>
      val v0 = value.get
      if (m.rcvrKey == v0.rcvrKey) {
        // Обновить состояние.
        val v2 = v0.withReq(
          m.tryRes.fold(
            v0.req.fail,
            v0.req.ready
          )
        )
        updated( Some(v2) )

      } else {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m.rcvrKey )
        noChange
      }


    // Сигнал сокрытия попапа с ошибкой или please-wait попапа, связанного с текущим действом, закрытия всех попапов.
    case _: ILkCommonPopupCloseAction if value.nonEmpty =>
      updated( None )

  }

}
