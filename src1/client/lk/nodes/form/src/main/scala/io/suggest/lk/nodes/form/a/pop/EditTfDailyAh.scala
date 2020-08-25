package io.suggest.lk.nodes.form.a.pop

import diode._
import io.suggest.bill.{Amount_t, MPrice}
import io.suggest.bill.tf.daily.{ITfDailyMode, InheritTf, MTfDailyInfo, ManualTf}
import io.suggest.cal.m.MCalTypes
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

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
  private def _nodeTfOpt2mia(nodeTfOpt: Option[MTfDailyInfo]): (MTextFieldS, MPrice) = {
    val priceOpt = nodeTfOpt
      .flatMap { tf =>
        tf.clauses
          .get( MCalTypes.All )
          .orElse( tf.clauses.values.headOption )
      }
    val priceAmount = priceOpt
      .fold[Amount_t]( 1 )( _.amount )
    val mprice = priceOpt.getOrElse {
      MPrice(priceAmount, nodeTfOpt.get.currency )
    }
    val amountStr = MPrice.amountStr( mprice )
    val mia = MTextFieldS(
      value   = amountStr,
      isValid = true
    )
    (mia, mprice)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал редактирования amount.
    case m: TfDailyManualAmountChanged =>
      val v0Opt = value
      val v2Opt = for (v0 <- v0Opt) yield {
        // TODO Нужна поддержка пустого значения.
        val (mode2, isValid) = try {
          val realAmount2 = m.amount
            .trim
            .replace(',' , '.')
            .toDouble

          val amount2 = MPrice.realAmountToAmount( realAmount2, v0.nodeTfOpt.get.currency )
          val mode22 = v0.mode.manualOpt
            .fold( ManualTf(amount2) )( _.withAmount(amount2) )
          mode22 -> true
        } catch {
          case _: Throwable =>
            v0.mode -> false
        }
        val inputAmount2 = Some(
          MTextFieldS(
            value   = m.amount,
            isValid = isValid
          )
        )
        v0.copy(
          mode          = mode2,
          inputAmount   = inputAmount2,
        )
      }
      updated( v2Opt )


    // Сигнал запуска редактирования посуточного тарифа. Инициализировать состояние редактора тарифа.
    case TfDailyEditClick =>
      val tree = treeRO()

      (for {
        loc0 <- tree.openedLoc
        currNode = loc0.getLabel
      } yield {
        val currNodeTfOpt = currNode.info.tf

        val mode0 = currNodeTfOpt.fold [ITfDailyMode] {
          // Should never happen: сервер забыл передать данные по тарифу.
          logger.warn( ErrorMsgs.TF_UNDEFINED, msg = currNode.info )
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
      })
        .getOrElse( noChange )


    case m: TfDailyModeChanged =>
      (for {
        v0 <- value
        currModeId = v0.mode.modeId
        if currModeId !=* m.modeId
        modF <- Option {
          if (m.modeId ==* ITfDailyMode.ModeId.Inherit) {
            // юзер выбрал режим наследования тарифа
            MEditTfDailyS.mode.set( InheritTf )
          } else if (m.modeId ==* ITfDailyMode.ModeId.Manual) {
            // выбран ручной режим управления тарифом.
            val (mia, mprice) = _nodeTfOpt2mia( v0.nodeTfOpt )
            MEditTfDailyS.mode.set( ManualTf( mprice.amount ) ) andThen
              MEditTfDailyS.inputAmount.set( Some(mia) )
          } else {
            logger.error( ErrorMsgs.TF_UNDEFINED, msg = m )
            null
          }
        }
      } yield {
        val v2 = modF(v0)
        updated( Some(v2) )
      })
        .getOrElse(noChange)


    // Сигнал отмены редактирования тарифа.
    case TfDailyCancelClick =>
      updated( None )

    case TfDailySaveClick =>
      (for {
        v0 <- value
        if {
          val r = v0.isValid
          // Should never happen: isValid вызывается в шаблоне.
          if (!r) logger.log( ErrorMsgs.VALIDATION_FAILED, msg = v0 )
          r
        }
        rcvrKey <- treeRO().openedRcvrKey
      } yield {
        // Запрос на сервер:
        val fx = Effect {
          api
            .setTfDaily( rcvrKey, v0.mode )
            .transform { tryRes =>
              Success(TfDailySavedResp(tryRes))
            }
        }

        // Обновление состояния:
        val v2 = MEditTfDailyS.request.modify(_.pending())(v0)

        updated(Some(v2), fx)
      })
        .getOrElse( noChange )


    // Сигнал завершения реквеста к серверу.
    case m: TfDailySavedResp =>
      value.fold {
        logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange

      } { v0 =>
        m.tryResp.fold(
          {ex =>
            val v2 = MEditTfDailyS.request.modify( _.fail(ex) )(v0)
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
