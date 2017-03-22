package io.suggest.lk.nodes.form.a.pop

import diode._
import io.suggest.bill.Amount_t
import io.suggest.bill.tf.daily.{ITfDailyMode, InheritTf, ManualTf}
import io.suggest.cal.m.MCalTypes
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 22:00
  * Description: ActionHandler обработки событий, связанных с редактированием посуточного тарифа узла.
  */
class EditTfDailyAh[M](
                        api         : ILkNodesApi,
                        modelRW     : ModelRW[M, Option[MEditTfDailyS]],
                        treeRO      : ModelRO[MTree]
                      )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал редактирования amount.
    case m: TfDailyManualAmountChanged =>
      val v0 = value
      val v2 = v0.map { v =>
        v.withMode(
          ManualTf(
            amount = m.amount.trim.toDouble
          )
        )
      }
      updated( v2 )


    // Сигнал запуска редактирования посуточного тарифа. Инициализировать состояние редактора тарифа.
    case TfDailyEditClick =>
      val tree = treeRO()
      val currNode = MNodeState.findSubNode(tree.showProps.get, tree.nodes).get
      val currNodeTfOpt = currNode.info.tf

      val mode0 = currNodeTfOpt.fold [ITfDailyMode] {
        // Should never happen: сервер забыл передать данные по тарифу.
        LOG.warn( ErrorMsgs.TF_UNDEFINED, msg = currNode.info )
        InheritTf
      } { tf0 =>
        tf0.mode
      }

      val v2 = MEditTfDailyS(
        mode    = mode0,
        nodeTfOpt  = currNodeTfOpt
      )
      updated( Some(v2) )


    // Сигнал о том, что юзер выбрал режим наследования тарифа.
    case TfDailyInheritedMode =>
      val v2 = for (s <- value) yield {
        s.withMode( InheritTf )
      }
      updated( v2 )

    // Сигнал, что юзер выбрал ручной режим управления тарифом.
    case TfDailyManualMode =>
      val v2 = for (s <- value) yield {
        val priceAmount = s.nodeTfOpt
          .flatMap { tf =>
            tf.clauses
              .get( MCalTypes.All )
              .orElse( tf.clauses.values.headOption )
          }
          .fold[Amount_t](1.0) { _.amount }
        s.withMode( ManualTf(priceAmount) )
      }
      updated( v2 )


    // Сигнал отмены редактирования тарифа.
    case TfDailyCancelClick =>
      updated( None )

    case TfDailySaveClick =>
      val v0 = value.get
      if (!v0.isValid) {
        // Should never happen: isValid вызывается в шаблоне.
        LOG.log( WarnMsgs.VALIDATION_FAILED, msg = v0 )
        noChange

      } else {
        val rcvrKey = treeRO().showProps.get

        // Запрос на сервер:
        val fx = Effect {
          api.setTfDaily(rcvrKey, v0.mode).transform { tryRes =>
            Success(TfDailySavedResp(tryRes))
          }
        }

        // Обновление состояния:
        val v2 = v0.withRequest(v0.request.pending())

        updated(Some(v2), fx)
      }


    // Сигнал завершения реквеста к серверу.
    case m: TfDailySavedResp =>
      value.fold {
        LOG.warn( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange

      } { v0 =>
        m.tryResp.fold(
          {ex =>
            val v2 = v0.withRequest( v0.request.fail(ex) )
            updated( Some(v2) )
          },
          { _ =>
            // Новые данные по узлу будут залиты в TreeAh.
            updated(None)
          }
        )
      }


  }

}
