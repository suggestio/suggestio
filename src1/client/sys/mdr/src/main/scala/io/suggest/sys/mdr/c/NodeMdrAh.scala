package io.suggest.sys.mdr.c

import diode.data.PendingBase
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.msg.WarnMsgs
import io.suggest.sys.mdr.MdrSearchArgs
import io.suggest.sys.mdr.m._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sys.mdr.v.NodeRenderR
import japgolly.univeq._

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

    // Ввод текста причины отказа в размещении.
    case m: SetDismissReason =>
      val v0 = value
      v0.dialogs.refuse
        .filter(_.reason !=* m.reason)
        .fold(noChange) { refuse0 =>
          val v2 = v0.withDialogs(
            v0.dialogs.withRefuse(
              Some( refuse0.withReason( m.reason ) )
            )
          )
          updated(v2)
        }

    // Нажимание кнопок аппрува или отказа в списке размещений.
    case m: ApproveOrDismiss =>
      val v0 = value
      if (m.isApprove) {
        // Аппрув - немедленный эффект запроса на сервер.
        println("TODO") // TODO
        noChange

      } else {
        // Отказ - нужен диалог отказа с указанием причины отказа.
        val v2 = v0.withDialogs(
          v0.dialogs.withRefuse(
            Some( MMdrRefuseDialogS(
              actionInfo = m.info
            ))
          )
        )
        updated(v2)
      }


    // В диалоге отказа нажата кнопка отмены:
    case DismissCancelClick =>
      val v0 = value
      v0.dialogs.refuse.fold {
        // Дублирующееся событие, диалог уже закрыт.
        noChange
      } { _ =>
        val v2 = v0.withDialogs(
          v0.dialogs
            .withRefuse(None)
        )
        updated(v2)
      }


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
        val jdCss2 = NodeRenderR.mkJdCss(v0.jdCss)(
          infoReq2
            .iterator
            .flatten
            .flatMap(_.ad)
            .map(_.template)
            .toSeq: _*
        )
        val v2 = v0
          .withInfo( infoReq2 )
          .withJdCss( jdCss2 )
        updated( v2 )

      } else {
        // Левый ответ какой-то, уже другой запрос запущен.
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
