package io.suggest.sc.c.dia

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Circuit, Effect, ModelRO, ModelRW}
import io.suggest.lk.m.CsrfTokenEnsure
import io.suggest.lk.nodes.MLknConf
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.nodes.form.m.{MLkNodesRoot, MTree, TreeInit}
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
                       csrfRO             : ModelRO[Pot[MCsrfToken]],
                     )
  extends ActionHandler( modelRW )
  with Log
{

  private def _nodesCircuitInitFx(nodesCircuit: Circuit[_]): Effect = {
    Effect.action {
      nodesCircuit.dispatch( TreeInit() )
      DoNothing
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

  def scNodesCircuitInit() = {
    // Минимальное начальное состояние:
    val lknRoot = MLkNodesRoot(
      conf = MLknConf(
        onNodeId = sc3Circuit.inxStateRO.value.rcvrId,
        adIdOpt  = None,
      ),
      tree = MTree(
        // Сразу ставим pending, чтобы была крутилка, несмотря на отсутствие начальных эффектов.
        nodes = Pot.empty.pending(),
      ),
    )
    ActionResult( Some(lknRoot), None )
  }

}
