package io.suggest.grid

import com.softwaremill.macwire._
import io.suggest.grid.build.GridBuilderJs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 18:27
  * Description: Compile-time DI support.
  */
class GridModule {

  lazy val gridBuilder = wire[GridBuilderJs]

}
