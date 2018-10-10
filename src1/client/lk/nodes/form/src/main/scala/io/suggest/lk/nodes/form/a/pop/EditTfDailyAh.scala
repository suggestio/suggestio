package io.suggest.lk.nodes.form.a.pop

import diode._
import io.suggest.bill.{Amount_t, MPrice}
import io.suggest.bill.tf.daily.{ITfDailyMode, InheritTf, MTfDailyInfo, ManualTf}
import io.suggest.cal.m.MCalTypes
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.log.Log
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

  /** Подготовить данные для рендера на основе тарифа. */
  private def _nodeTfOpt2mia(nodeTfOpt: Option[MTfDailyInfo]): (MInputAmount, MPrice) = {
    val priceOpt = nodeTfOpt
      .flatMap { tf =>
        tf.clauses
          .get( MCalTypes.All )
          .orElse( tf.clauses.values.headOption )
      }
    val priceAmount = priceOpt
      .fold[Amount_t](1) { _.amount }
    val mprice = priceOpt.getOrElse {
      MPrice(priceAmount, nodeTfOpt.get.currency )
    }
    val amountStr = MPrice.amountStr( mprice )
    val mia = MInputAmount(
      value   = amountStr,
      isValid = true
    )
    (mia, mprice)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал редактирования amount.
    case m: TfDailyManualAmountChanged =>
      val v0 = value
      val v2 = for (v <- v0) yield {
        val (mode2, isValid) = try {
          val amount2 = m.amount
            .trim
            .replace(',' , '.')
            .toLong
          val mode22 = v.mode.manualOpt
            .fold( ManualTf(amount2) )( _.withAmount(amount2) )
          mode22 -> true
        } catch {
          case _: Throwable =>
            v.mode -> false
        }
        v.withModeInputAmount(
          mode2,
          Some(
            MInputAmount(
              value   = m.amount,
              isValid = isValid
            )
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
      val (mia, _) = _nodeTfOpt2mia(currNodeTfOpt)

      val v2 = MEditTfDailyS(
        mode       = mode0,
        nodeTfOpt  = currNodeTfOpt,
        inputAmount = Some( mia )
      )
      updated( Some(v2) )


    // Сигнал о том, что юзер выбрал режим наследования тарифа.
    case TfDailyInheritedMode =>
      val v2 = for (s <- value) yield {
        s.withModeInputAmount( InheritTf, None )
      }
      updated( v2 )

    // Сигнал, что юзер выбрал ручной режим управления тарифом.
    case TfDailyManualMode =>
      val v2 = for (s <- value) yield {
        val (mia, mprice) = _nodeTfOpt2mia( s.nodeTfOpt )
        s.withModeInputAmount( ManualTf(mprice.amount), Some(mia) )
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
