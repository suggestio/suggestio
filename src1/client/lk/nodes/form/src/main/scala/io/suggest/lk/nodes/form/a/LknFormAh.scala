package io.suggest.lk.nodes.form.a

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.m.DeleteConfirmPopupCancel
import io.suggest.lk.nodes.MLknConf
import io.suggest.lk.nodes.form.m.{CreateNodeCloseClick, MLkNodesRoot, NodeEditCancelClick, NodesDiConf, SetAd, TfDailyCancelClick, TreeInit}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.{DoNothing, HwBackBtn}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.09.2020 21:21
  * Description: Контроллер lkn-формы верхнего уровня. Отрабатывает глобальные операции.
  */
class LknFormAh[M](
                    modelRW   : ModelRW[M, MLkNodesRoot],
                    diConf    : NodesDiConf,
                  )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Смена режима работы формы или смена текущей карточки.
    case m: SetAd =>
      val v0 = value
      if (v0.conf.adIdOpt ==* m.adId) {
        noChange

      } else {
        val v2 = MLkNodesRoot.conf
          .composeLens( MLknConf.adIdOpt )
          .set( m.adId )(v0)

        // Запустить пере-инициализацию дерева:
        val fx = TreeInit().toEffectPure
        updated(v2, fx)
      }


    // Из выдачи сюда проброшено нажатие кнопки "Назад".
    case m @ HwBackBtn =>
      val v0 = value

      v0.popups
        .createNodeS
        .map { _ =>
          // Закрыть диалог создания узла.
          effectOnly( CreateNodeCloseClick.toEffectPure )
        }
        .orElse {
          v0.popups
            .deleteNodeS
            .map { _ =>
              effectOnly( DeleteConfirmPopupCancel.toEffectPure )
            }
        }
        .orElse {
          v0.popups
            .editName
            .map { _ =>
              effectOnly( NodeEditCancelClick.toEffectPure )
            }
        }
        .orElse {
          v0.popups
            .editTfDailyS
            .map { _ =>
              effectOnly( TfDailyCancelClick.toEffectPure )
            }
        }
        .orElse {
          diConf
            .closeForm
            .map { closeFormCb =>
              val fx = Effect.action {
                closeFormCb.runNow()
                DoNothing
              }
              effectOnly(fx)
            }
        }
        .getOrElse {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.popups) )
          noChange
        }

  }

}
