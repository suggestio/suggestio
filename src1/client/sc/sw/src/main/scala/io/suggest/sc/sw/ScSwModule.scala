package io.suggest.sc.sw

import com.softwaremill.macwire._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.18 21:31
  * Description: macwire-линковка ServiceWorker'а выдачи.
  */
class ScSwModule {

  val circuit = wire[ScSwCircuit]

}
