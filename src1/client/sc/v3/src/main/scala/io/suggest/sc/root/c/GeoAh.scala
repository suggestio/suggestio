package io.suggest.sc.root.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.root.m.MScGeo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.12.17 16:46
  * Description: Контроллер геолокации.
  */
class GeoAh[M](
                modelRW     : ModelRW[M, MScGeo]
              )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    ???
  }

}
