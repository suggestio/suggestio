package io.suggest.lk.nodes.form.a.tree

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.nodes.form.m.{MTree, NodeNameClick}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:50
  * Description: Diode action-handler
  */
class TreeAh[M](
                 modelRW: ModelRW[M, MTree]
               )
  extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о необходимости показать какой-то узел подробнее.
    case nnc: NodeNameClick =>
      println("clicked: " + nnc.rcvrKey)
      // TODO Надо отправить запрос к серверу за данными по выбранному узлу, выставить ожидание ответа в состояние.
      ???

  }

}
