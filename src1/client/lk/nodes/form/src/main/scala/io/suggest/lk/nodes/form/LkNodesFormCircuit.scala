package io.suggest.lk.nodes.form

import diode.react.ReactConnector
import io.suggest.bin.ConvCodecs
import io.suggest.lk.nodes.MLknFormInit
import io.suggest.lk.nodes.form.a.tree.TreeAh
import io.suggest.lk.nodes.form.m.{MLkNodesRoot, MNodeState, MTree}
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.{ErrorMsg_t, ErrorMsgs}
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

  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.LK_NODES_FORM_ERROR

  /** Сборка начального инстанса корневой модели. */
  override protected def initialModel: MLkNodesRoot = {
    val stateInp = StateInp.find().get
    val base64   = stateInp.value.get
    val mFormInit = PickleUtil.unpickleConv[String, ConvCodecs.Base64, MLknFormInit](base64)
    val mroot = MLkNodesRoot(
      tree = MTree(
        nodes = for (nInfo <- mFormInit.nodes0.nodes) yield {
          MNodeState(nInfo)
        }
      )
    )
    // Потом удалить input, который больше не нужен.
    Future {
      stateInp.remove()
    }
    // Наконец вернуть собранную root-модель.
    mroot
  }


  override protected def actionHandler: HandlerFunction = {
    // Реагировать на события древа узлов.
    val treeAh = new TreeAh(
      modelRW = zoomRW(_.tree) { _.withTree(_) }
    )

    treeAh
  }

}
