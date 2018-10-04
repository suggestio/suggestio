package io.suggest.sys.mdr.c

import diode.data.PendingBase
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.msg.WarnMsgs
import io.suggest.sys.mdr.MdrSearchArgs
import io.suggest.sys.mdr.m.{MSysMdrRootS, MdrNextNode, MdrNextNodeResp}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:30
  * Description: Контроллер формы модерации.
  */
class NodeMdrAh[M](
                    api       : ISysMdrApi,
                    modelRW   : ModelRW[M, MSysMdrRootS]
                  )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Переход к следующему узлу, который требует модерации.
    case MdrNextNode =>
      val v0 = value
      if (v0.info.isPending) {
        noChange

      } else {
        val infoReq2 = v0.info.pending()

        // Организовать запрос на сервер:
        val fx = Effect {
          val args = MdrSearchArgs(
            // Пропустить текущую карточку:
            hideAdIdOpt = v0.info
              .toOption
              .flatten
              .map(_.nodeId)
          )
          api.nextMdrInfo(args)
            .transform { tryResp =>
              val r = MdrNextNodeResp(
                timestampMs   = infoReq2.asInstanceOf[PendingBase].startTime,
                tryResp       = tryResp
              )
              Success(r)
            }
        }

        // И результат экшена...
        val v2 = v0
          .withInfo( infoReq2 )
        updated(v2 , fx)
      }

    // Поступил результат реквеста к серверу за новыми данными для модерации.
    case m: MdrNextNodeResp =>
      val v0 = value
      if (v0.info isPendingWithStartTime m.timestampMs) {
        // Это ожидаемый ответ сервера. Обработать его:
        val infoReq2 = m.tryResp.fold(
          v0.info.fail,
          v0.info.ready
        )
        val v2 = v0.withInfo( infoReq2 )
        updated( v2 )

      } else {
        // Левый ответ какой-то, уже другой запрос запущен.
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
