package io.suggest.lk.nodes.form

import diode.FastEq
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.lk.nodes.MLknFormInit
import io.suggest.lk.nodes.form.a.LkNodesApiHttpImpl
import io.suggest.lk.nodes.form.a.pop.{CreateNodeAh, DeleteNodeAh, EditTfDailyAh, NameEditAh}
import io.suggest.lk.nodes.form.a.tree.TreeAh
import io.suggest.lk.nodes.form.m.{MLkNodesRoot, MLknPopups, MNodeState, MTree}
import io.suggest.log.CircuitLog
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.lk.nodes.form.m.MCreateNodeS.MCreateNodeSFastEq
import io.suggest.lk.nodes.form.m.MEditTfDailyS.MTfDailyEditSFastEq
import io.suggest.lk.nodes.form.m.MTree.MTreeFastEq
import io.suggest.lk.nodes.form.m.MLknPopups.MLknPopupsFastEq
import io.suggest.lk.m.MDeleteConfirmPopupS.MDeleteConfirmPopupSFastEq
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.{CircuitUtil, StateInp}
import play.api.libs.json.Json
import japgolly.univeq._
import io.suggest.scalaz.ZTreeUtil._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 16:10
  * Description: Diode circuit для формы управления узлами в личном кабинете.
  */
class LkNodesFormCircuit extends CircuitLog[MLkNodesRoot] with ReactConnector[MLkNodesRoot] {

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.LK_NODES_FORM_ERROR

  /** Сборка начального инстанса корневой модели. */
  override protected def initialModel: MLkNodesRoot = {
    val stateInp = StateInp.find().get
    val base64   = stateInp.value.get
    val mFormInit = Json.parse(base64).as[MLknFormInit]

    val tree = mFormInit.resp0
      .subTree
      .map { lknNode =>
        MNodeState( Pot.empty.ready(lknNode) )
      }
    val mroot = MLkNodesRoot(
      conf = mFormInit.conf,
      tree = {
        MTree(
          nodes = tree,
          opened = tree
            .loc
            .find(_.getLabel.info.id ==* mFormInit.conf.onNodeId)
            .map(_.toNodePath),
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

    val confR = CircuitUtil.mkLensRootZoomRO(this, MLkNodesRoot.conf)
    val treeRW = CircuitUtil.mkLensRootZoomRW(this, MLkNodesRoot.tree)
    val popupsRW = CircuitUtil.mkLensRootZoomRW(this, MLkNodesRoot.popups)
    val currNodeRO = treeRW.zoom(_.openedRcvrKey)( FastEq.ValueEq )
    val openedPathRO = CircuitUtil.mkLensZoomRO( treeRW, MTree.opened )

    // Реагировать на события из дерева узлов.
    val treeAh = new TreeAh(
      api     = API,
      modelRW = treeRW,
      confRO  = confR
    )

    // Реактор на события, связанные с окошком создания узла.
    val createNodeAh = new CreateNodeAh(
      api         = API,
      modelRW     = CircuitUtil.mkLensZoomRW( popupsRW, MLknPopups.createNodeS ),
      currNodeRO  = currNodeRO
    )

    // Реактор на события, связанные с окошком удаления узла.
    val deleteNodeAh = new DeleteNodeAh(
      api         = API,
      modelRW     = CircuitUtil.mkLensZoomRW( popupsRW, MLknPopups.deleteNodeS ),
      currNodeRO  = currNodeRO,
      openedPathRO = openedPathRO,
    )

    // Реактор на события редактирования тарифа узла.
    val editTfDailyAh = new EditTfDailyAh(
      api     = API,
      modelRW = CircuitUtil.mkLensZoomRW( popupsRW, MLknPopups.editTfDailyS ),
      treeRO  = treeRW
    )

    val nameEditAh = new NameEditAh(
      api = API,
      modelRW = CircuitUtil.mkLensZoomRW( popupsRW, MLknPopups.editName ),
      currNodeRO = treeRW.zoom(_.openedLoc.map(_.getLabel)),
    )

    val popupsHandler = composeHandlers( createNodeAh, deleteNodeAh, editTfDailyAh, nameEditAh )
    // Разные Ah шарят между собой некоторые события, поэтому они все соединены параллельно.
    foldHandlers(treeAh, popupsHandler)
  }

  //addProcessor( io.suggest.spa.LoggingAllActionsProcessor[MLkNodesRoot] )

}
