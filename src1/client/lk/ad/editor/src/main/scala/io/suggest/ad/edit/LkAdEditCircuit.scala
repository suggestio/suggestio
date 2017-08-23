package io.suggest.ad.edit

import diode.react.ReactConnector
import io.suggest.ad.edit.m.{MAdEditFormInit, MAdEditRoot, MDocS}
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.spa.StateInp
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:28
  * Description: Diode-circuit редактора рекламных карточек второго поколения.
  */
object LkAdEditCircuit extends CircuitLog[MAdEditRoot] with ReactConnector[MAdEditRoot] {

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.AD_EDIT_CIRCUIT_ERROR

  override protected def initialModel: MAdEditRoot = {
    // Найти на странице текстовое поле с сериализованным состянием формы.
    val stateInp = StateInp.find().get
    val jsonStr = stateInp.value.get
    val mFormInit = Json
      .parse( jsonStr )
      .as[MAdEditFormInit]

    MAdEditRoot(
      conf = mFormInit.conf,
      doc  = MDocS(
        template = mFormInit.form.template,
        edges = IId.els2idMap( mFormInit.form.edges )
      )
    )
  }


  override protected def actionHandler: HandlerFunction = {
    ???
  }

}
