package io.suggest.lk.nodes.form.a.tree

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m.{HandleSubNodesOf, MTree, NodeNameClick}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:50
  * Description: Diode action-handler
  */
class TreeAh[M](
                 api      : ILkNodesApi,
                 modelRW  : ModelRW[M, MTree]
               )
  extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о необходимости показать какой-то узел подробнее.
    case nnc: NodeNameClick =>

      val rcvrKey = nnc.rcvrKey
      val nodeId = nnc.rcvrKey.head

      // Собрать эффект запроса к серверу за подробностями по узлу.
      val fx = Effect {
        // Отправить запрос к серверу за данными по выбранному узлу, выставить ожидание ответа в состояние.
        for {
          resp <- api.subNodesOf(nodeId)
        } yield {
          HandleSubNodesOf(rcvrKey, resp)
        }
      }

      val v0 = value
      println("clicked: " + nnc.rcvrKey)
      ???

  }

}
