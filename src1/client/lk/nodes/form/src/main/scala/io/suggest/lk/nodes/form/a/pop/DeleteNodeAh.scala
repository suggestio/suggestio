package io.suggest.lk.nodes.form.a.pop

import diode._
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.lk.m.{DeleteConfirmPopupCancel, DeleteConfirmPopupOk, MDeleteConfirmPopupS}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.scalaz.NodePath_t
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.17 18:44
  * Description: ActionHandler для подсистемы удаления узла.
  */
class DeleteNodeAh[M](
                       api        : ILkNodesApi,
                       modelRW    : ModelRW[M, Option[MDeleteConfirmPopupS]],
                       currNodeRO : ModelRO[Option[RcvrKey]],
                       openedPathRO: ModelRO[Option[NodePath_t]],
                     )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал нажатия на кнопку "удалить" возле какого-то узла. Выставить флаг отображения формы удаления узла.
    case NodeDeleteClick =>
      updated( Some(MDeleteConfirmPopupS()) )


    // Сигнал подтверждения удаления узла.
    case DeleteConfirmPopupOk =>
      val v0 = value.get
      val v2 = MDeleteConfirmPopupS.request
        .modify( _.pending() )(v0)

      // Запустить удаление узла на сервере.
      val fx = Effect {
        val rcvrKey = currNodeRO().get
        val nodePath = openedPathRO().get
        val nodeId = rcvrKey.last
        api
          .deleteNode( nodeId )
          .transform { tryRes =>
            val r = NodeDeleteResp(nodePath, nodeId, tryRes)
            Success(r)
          }
      }

      updated(Some(v2), fx)


    // Сигнал о завершении запроса к серверу по поводу удаления узла.
    case _: NodeDeleteResp =>
      updated( None )


    // Сигнал отмены удаления узла. Скрыть диалог удаления.
    case DeleteConfirmPopupCancel =>
      updated( None )

  }

}
