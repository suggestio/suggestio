package io.suggest.sys.mdr.c

import diode.data.{PendingBase, Pot}
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.msg.WarnMsgs
import io.suggest.sys.mdr.{MMdrActionInfo, MMdrResolution, MdrSearchArgs}
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

  /** Эффект отправки mdr-команды на сервер с апдейтом состояния. pure-функция. */
  def _doMdrFx(info: MMdrActionInfo, reasonOpt: Option[String], v0: MSysMdrRootS): (MSysMdrRootS, Effect) = {
    val pot0 = v0.mdrPots.getOrElse(info, Pot.empty)
    val pot2 = pot0.pending()
    val fx = Effect {
      val startTimestampMs = pot2.asInstanceOf[PendingBase].startTime
      api.doMdr(
        MMdrResolution(
          nodeId  = v0.info.get.get.nodeId,
          info    = info,
          reason  = None
        )
      ).transform { tryRes =>
        val act = DoMdrResp(
          timestampMs = startTimestampMs,
          info        = info,
          tryResp     = tryRes
        )
        Success(act)
      }
    }

    val mdrPots2 = v0.mdrPots + (info -> pot2)
    val v2 = v0.withMdrPots( mdrPots2 )
    (v2, fx)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Ввод текста причины отказа в размещении.
    case m: SetDismissReason =>
      val v0 = value
      if (v0.dialogs.refuse.reason ==* m.reason) {
        // Текст причины не изменился.
        noChange
      } else {
        val v2 = v0.withDialogs(
          v0.dialogs.withRefuse(
            v0.dialogs.refuse
              .withReason( m.reason )
          )
        )
        updated(v2)
      }


    // Результат запроса с командой модерации
    case m: DoMdrResp =>
      val v0 = value
      v0.mdrPots
        .get( m.info )
        .fold(noChange) { pot0 =>
          if (pot0 isPendingWithStartTime m.timestampMs) {
            // Это ожидаемый ответ сервера. Обработать его:
            val mdrPots2 = m.tryResp.fold(
              {ex =>
                v0.mdrPots + (m.info -> pot0.fail(ex))
              },
              {_ =>
                v0.mdrPots - m.info
              }
            )

            // Если открыт refuse-диалог, то закрыть его.
            val dia2 = if (m.tryResp.isSuccess && (v0.dialogs.refuse.actionInfo contains m.info)) {
              // Закрыть диалог, т.к. всё ок.
              v0.dialogs.withRefuse(
                v0.dialogs.refuse.withActionInfo( None )
              )
            } else {
              v0.dialogs
            }

            val v2 = v0.copy(
              dialogs = dia2,
              mdrPots = mdrPots2
            )
            updated(v2)

          } else {
            // Есть ещё какой-то запущенный запрос, который более актуален.
            LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
            noChange
          }
        }


    // Нажимание кнопок аппрува или отказа в списке размещений.
    case m: ApproveOrDismiss =>
      val v0 = value
      if (m.isApprove) {
        // Аппрув - немедленный эффект запроса на сервер.
        val (v2, fx) = _doMdrFx(m.info, reasonOpt = None, v0)
        updated(v2, fx)

      } else {
        // Отказ - нужен диалог отказа с указанием причины отказа.
        val v2 = v0.withDialogs(
          v0.dialogs.withRefuse(
            v0.dialogs.refuse
              .withActionInfo( Some(m.info) )
          )
        )
        updated(v2)
      }


    // В диалоге отказа нажато подтверждение
    case DismissOkClick =>
      val v0 = value
      val refuse = v0.dialogs.refuse
      refuse.actionInfo.fold(noChange) { ai =>
        val (v2, fx) = _doMdrFx(ai, Some(refuse.reason), v0)
        updated(v2, fx)
      }


    // В диалоге отказа нажата кнопка отмены:
    case DismissCancelClick =>
      val v0 = value
      v0.dialogs.refuse.actionInfo.fold {
        // Дублирующееся событие, диалог уже закрыт.
        noChange
      } { _ =>
        val v2 = v0.withDialogs(
          v0.dialogs.withRefuse(
            v0.dialogs.refuse.withActionInfo(None)
          )
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
        val jdCss2 = NodeRenderR.mkJdCss( Some(v0.jdCss) )(
          infoReq2
            .iterator
            .flatten
            .flatMap(_.ad)
            .map(_.template)
            .toSeq: _*
        )
        val v2 = v0.copy(
          info    = infoReq2,
          jdCss   = jdCss2,
          mdrPots = Map.empty,
          // TODO Закрыть все открытые диалоги
        )
        updated( v2 )

      } else {
        // Левый ответ какой-то, уже другой запрос запущен.
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
