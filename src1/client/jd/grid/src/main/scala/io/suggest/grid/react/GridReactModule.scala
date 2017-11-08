package io.suggest.grid.react

import com.softwaremill.macwire._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 18:27
  * Description: Compile-time DI support.
  */
class GridReactModule {

  lazy val gridBuilder = wire[GridBuilder]

}
