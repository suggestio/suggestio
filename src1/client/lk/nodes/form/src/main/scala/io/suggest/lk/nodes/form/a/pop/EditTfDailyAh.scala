package io.suggest.lk.nodes.form.a.pop

import diode.{ActionHandler, ActionResult, ModelRO, ModelRW}
import io.suggest.bill.tf.daily.{ITfDailyMode, InheritTf}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs

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

    // Сигнал запуска редактирования посуточного тарифа. Инициализировать состояние редактора тарифа.
    case TfDailyEditClick =>
      val tree = treeRO()
      val currNode = MNodeState.findSubNode(tree.showProps.get, tree.nodes).get
      val mode0 = currNode.info.tf.fold [ITfDailyMode] {
        // Should never happen: сервер забыл передать данные по тарифу.
        LOG.warn( ErrorMsgs.TF_UNDEFINED, msg = currNode.info )
        InheritTf
      } { tf0 =>
        tf0.mode
      }

      val v2 = MEditTfDailyS(
        mode = mode0
      )
      updated( Some(v2) )

      
    // Сигнал отмены редактирования тарифа.
    case TfDailyCancelClick =>
      updated( None )

  }

}