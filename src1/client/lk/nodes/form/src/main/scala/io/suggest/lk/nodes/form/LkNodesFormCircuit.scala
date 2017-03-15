package io.suggest.lk.nodes.form

import diode.data.Ready
import diode.react.ReactConnector
import io.suggest.bin.ConvCodecs
import io.suggest.lk.nodes.MLknFormInit
import io.suggest.lk.nodes.form.a.LkNodesApiHttpImpl
import io.suggest.lk.nodes.form.a.tree.TreeAh
import io.suggest.lk.nodes.form.m.{MLkNodesRoot, MLknOther, MNodeState, MTree}
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

  val API = new LkNodesApiHttpImpl

  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.LK_NODES_FORM_ERROR


  /** Сборка начального инстанса корневой модели. */
  override protected def initialModel: MLkNodesRoot = {
    val stateInp = StateInp.find().get
    val base64   = stateInp.value.get
    val mFormInit = PickleUtil.unpickleConv[String, ConvCodecs.Base64, MLknFormInit](base64)

    val mroot = MLkNodesRoot(
      other = MLknOther(
        onNodeId = mFormInit.onNodeId,
        adIdOpt  = mFormInit.adIdOpt
      ),
      tree = {
        val chStates = for (nInfo <- mFormInit.nodes0.children) yield {
          MNodeState(nInfo)
        }
        MTree(
          nodes = mFormInit.nodes0.info.fold(chStates) { parentInfo =>
            val parent = MNodeState(
              info      = parentInfo,
              children  = Ready(chStates)
            )
            parent :: Nil
          },
          showProps = Some( mFormInit.onNodeId :: Nil )
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


  override protected def actionHandler: HandlerFunction = {
    //val confR = zoom(_.other)

    // Реагировать на события древа узлов.
    val treeAh = new TreeAh(
      api     = API,
      modelRW = zoomRW(_.tree) { _.withTree(_) }
    )

    treeAh
  }

}
