package io.suggest.ws.pool

import diode.Effect

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 22:08
  */
package object m {

  type WsCallbackF = Any => Option[Effect]

}
