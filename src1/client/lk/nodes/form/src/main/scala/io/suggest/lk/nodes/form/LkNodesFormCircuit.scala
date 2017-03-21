package io.suggest.lk.nodes.form

import diode.react.ReactConnector
import io.suggest.bin.ConvCodecs
import io.suggest.lk.nodes.MLknFormInit
import io.suggest.lk.nodes.form.a.LkNodesApiHttpImpl
import io.suggest.lk.nodes.form.a.pop.{CreateNodeAh, DeleteNodeAh}
import io.suggest.lk.nodes.form.a.tree.TreeAh
import io.suggest.lk.nodes.form.m.{MLkNodesRoot, MNodeState, MTree}
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.spa.StateInp
import io.suggest.sjs.common.bin.EvoBase64JsUtil.EvoBase64JsDecoder
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 16:10
  * Description: Diode circuit для формы управления узлами в личном кабинете.
  */
object LkNodesFormCircuit extends CircuitLog[MLkNodesRoot] with ReactConnector[MLkNodesRoot] {

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.LK_NODES_FORM_ERROR

  /** Сборка начального инстанса корневой модели. */
  override protected def initialModel: MLkNodesRoot = {
    val stateInp = StateInp.find().get
    val base64   = stateInp.value.get
    val mFormInit = PickleUtil.unpickleConv[String, ConvCodecs.Base64, MLknFormInit](base64)

    val mroot = MLkNodesRoot(
      conf = mFormInit.conf,
      tree = {
        MTree(
          nodes     = mFormInit.nodes0.map(MNodeState.apply),
          showProps = Some( mFormInit.conf.onNodeId :: Nil )
        )
      }
    )

    // Потом удалить input, который больше не нужен.
    Future {
      stateInp.remove()
    }

    // Наконец вернуть собранную root-модель.
    mroot
  }


  override protected val actionHandler: HandlerFunction = {
    val API = new LkNodesApiHttpImpl

    val confR = zoom(_.conf)
    val treeRW = zoomRW(_.tree) { _.withTree(_) }
    val popupsRW = zoomRW(_.popups) { _.withPopups(_) }
    val currNodeR = treeRW.zoom(_.showProps)

    // Реагировать на события из дерева узлов.
    val treeAh = new TreeAh(
      api     = API,
      modelRW = treeRW,
      confRO  = confR
    )

    // Реактор на события, связанные с окошком создания узла.
    val createNodeAh = new CreateNodeAh(
      api         = API,
      modelRW     = popupsRW.zoomRW(_.createNodeS) { _.withCreateNodeState(_) },
      currNodeRO  = currNodeR
    )

    // Реактор на события, связанные с окошком удаления узла.
    val deleteNodeAh = new DeleteNodeAh(
      api         = API,
      modelRW     = popupsRW.zoomRW(_.deleteNodeS) { _.withDeleteNodeState(_) },
      currNodeRO  = currNodeR
    )

    // Разные Ah шарят между собой некоторые события, поэтому они все соединены параллельно.
    foldHandlers(treeAh, createNodeAh, deleteNodeAh)
  }

}
