package io.suggest.sys.mdr.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sys.mdr.m.MSysMdrRootS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:30
  * Description: Контроллер формы модерации.
  */
class NodeMdrAh[M](
                    modelRW: ModelRW[M, MSysMdrRootS]
                  )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    ???
  }

}
