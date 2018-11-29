package io.suggest.sc.sw

import diode.Circuit
import io.suggest.sc.sw.c.ScSwRootAh
import io.suggest.sc.sw.m.MScSwRoot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.18 21:16
  * Description: FSM Circuit для ServiceWorker'а.
  */
class ScSwCircuit extends Circuit[MScSwRoot] {

  override protected def initialModel: MScSwRoot = {
    MScSwRoot()
  }


  // models
  val rootRW = zoomRW(identity)((_, v2) => v2)


  // controllers
  val rootAh = new ScSwRootAh(
    modelRW = rootRW
  )


  override protected val actionHandler: HandlerFunction = {
    rootAh
  }

}
