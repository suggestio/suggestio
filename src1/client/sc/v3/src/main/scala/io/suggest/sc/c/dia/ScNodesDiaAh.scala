package io.suggest.sc.c.dia

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Circuit, Effect, ModelRO, ModelRW}
import io.suggest.ble.BeaconsNearby_t
import io.suggest.lk.m.CsrfTokenEnsure
import io.suggest.lk.nodes.MLknConf
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.nodes.form.m.{BeaconsDetected, MLkNodesRoot, MTree, MTreeOuter, TreeInit}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.model.MCsrfToken
import io.suggest.sc.Sc3Module.sc3Circuit
import io.suggest.sc.m.ScNodesShowHide
import io.suggest.sc.m.dia.MScNodes
import io.suggest.spa.DoNothing

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.2020 12:18
  * Description: sc-контроллер над формой узлов.
  */
class ScNodesDiaAh[M](
                       getNodesCircuit    : () => LkNodesFormCircuit,
                       modelRW            : ModelRW[M, MScNodes],
                       beaconsNearbyRO    : ModelRO[BeaconsNearby_t],
                       csrfRO             : ModelRO[Pot[MCsrfToken]],
                       isLoggedInRO       : ModelRO[Boolean],
                     )
  extends ActionHandler( modelRW )
  with Log
{

  /** Эффект инициализации nodes circuit. */
  private def _nodesCircuitInitFx(nodesCircuit: Circuit[_]): Effect = {
    Effect.action {
      if (isLoggedInRO.value)
        nodesCircuit.dispatch( TreeInit() )

      val beaconsNearby = beaconsNearbyRO.value
      if (beaconsNearby.nonEmpty)
        nodesCircuit.dispatch( BeaconsDetected(beaconsNearby) )

      DoNothing
    }
  }

  /** Опциональный эффект обновления маячков в nodes circuit. */
  def onBeaconsUpdatedFx(beacons: BeaconsNearby_t): Option[Effect] = {
    for (circuit <- modelRW.value.circuit) yield {
      Effect.action {
        circuit.dispatch( BeaconsDetected(beacons) )
        DoNothing
      }
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Переключение видимости формы.
    case m: ScNodesShowHide =>
      val v0 = value

      if (m.visible) {
        v0.circuit.fold {
          val circuit = getNodesCircuit()

          val fx = if (csrfRO.value.isEmpty) {
            CsrfTokenEnsure(
              onComplete = Some( m.toEffectPure ),
            ).toEffectPure
          } else {
            _nodesCircuitInitFx( circuit )
          }

          val v2 = (MScNodes.circuit set Some(circuit))(v0)
          updated( v2, fx )

        } { nodesCircuit =>
          // Запуск эффекта инициализации начального дерева:
          val fx = _nodesCircuitInitFx( nodesCircuit )
          effectOnly( fx )
        }

      } else if (!m.visible && v0.circuit.nonEmpty) {
        val v2 = (MScNodes.circuit set None)(v0)
        updated( v2 )

      } else {
        logger.info( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.circuit) )
        noChange
      }

  }

}


object ScNodesDiaAh {

  def scNodesCircuitInit(userIsLoggedIn: Boolean): ActionResult[MLkNodesRoot] = {
    val nodes0 = MTree.emptyNodesTreePot

    // Минимальное начальное состояние:
    val lknRoot = MLkNodesRoot(
      conf = MLknConf(
        onNodeId = sc3Circuit.inxStateRO.value.rcvrId,
        adIdOpt  = None,
      ),
      tree = MTreeOuter(
        tree = MTree(
          // Для loggedIn-юзера сразу ставим pending, чтобы была крутилка - потом будет подгрузка узлов.
          nodes = if (userIsLoggedIn) nodes0.pending() else nodes0,
        ),
      ),
    )

    ActionResult( Some(lknRoot), None )
  }

}
