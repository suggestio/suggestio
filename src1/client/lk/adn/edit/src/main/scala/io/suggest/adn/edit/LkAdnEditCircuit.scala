package io.suggest.adn.edit

import diode.react.ReactConnector
import io.suggest.adn.edit.c.NodeEditAh
import io.suggest.adn.edit.m.{MAdnNodeS, MLkAdnEditRoot}
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.StateInp
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 17:35
  * Description: Circuit для формы редактирования ADN-узла.
  */
class LkAdnEditCircuit
  extends CircuitLog[MLkAdnEditRoot]
  with ReactConnector[MLkAdnEditRoot]
{

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.LK_ADN_EDIT_FORM_FAILED

  /** Извлекать начальное состояние формы из html-страницы. */
  override protected def initialModel: MLkAdnEditRoot = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val minit = Json.parse(json)
      .as[MAdnEditFormInit]

    MLkAdnEditRoot(
      conf = minit.conf,
      node = MAdnNodeS(
        meta          = minit.form.mmeta,
        colorPresets  = minit.form.mmeta.colors.allColorsIter.toList
      )
    )
  }


  // Models
  val nodeRW = zoomRW(_.node)(_.withNode(_))


  // Controllers
  val nodeEditAh = new NodeEditAh(
    modelRW = nodeRW
  )

  override protected val actionHandler: HandlerFunction = {
    nodeEditAh
  }

}
