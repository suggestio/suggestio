package io.suggest.lk.nodes.form.a.pop

import diode.{ActionHandler, ActionResult, ModelRO, ModelRW}
import io.suggest.lk.nodes.MLknConf
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m.MCreateNodeS
import io.suggest.sjs.common.log.Log

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 23:48
  * Description: Action handler для popup-формы создания узла.
  */
class CreateNodeAh[M](
                       api      : ILkNodesApi,
                       modelRW  : ModelRW[M, MCreateNodeS],
                       confRO   : ModelRO[MLknConf]
                     )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    ???
  }

}
