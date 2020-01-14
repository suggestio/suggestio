package io.suggest.n2.edge.edit

import diode.react.ReactConnector
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.{MEdge, MPredicates}
import io.suggest.n2.edge.edit.c.EdgeEditAh
import io.suggest.n2.edge.edit.m.MEdgeEditRoot
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.{CircuitUtil, StateInp}
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:50
  * Description: Circuit для формы заливки файла.
  */
class EdgeEditCircuit
  extends CircuitLog[MEdgeEditRoot]
  with ReactConnector[MEdgeEditRoot] {

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.FORM_ERROR

  override protected def initialModel: MEdgeEditRoot = {
    (for {
      stateInp    <- StateInp.find()
      stateValue  <- stateInp.value
      state0      <- Json
        .parse( stateValue )
        .asOpt[MEdgeEditFormInit]
    } yield {
      MEdgeEditRoot(
        edge = state0.edge getOrElse MEdge( MPredicates.values.head ),
        conf = state0.edgeId,
      )
    })
      .get
  }


  private val edgeRW = CircuitUtil.mkLensRootZoomRW(this, MEdgeEditRoot.edge)


  private val edgeEditAh = new EdgeEditAh(
    modelRW = edgeRW,
  )

  override protected val actionHandler: HandlerFunction = {
    edgeEditAh
  }

}
