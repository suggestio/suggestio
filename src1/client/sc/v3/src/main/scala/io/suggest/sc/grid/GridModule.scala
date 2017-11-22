package io.suggest.sc.grid

import com.softwaremill.macwire._
import io.suggest.jd.render.JdRenderModule
import io.suggest.sc.grid.v.{GridLoaderR, GridR}
import io.suggest.sc.styl.ScCssModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 20:25
  * Description: Поддержка DI для grid.
  */
class GridModule(
                  jdRenderModule  : JdRenderModule,
                  scCssModule     : ScCssModule
                ) {

  import jdRenderModule._
  import scCssModule._

  lazy val gridLoaderR = wire[GridLoaderR]

  lazy val gridR   = wire[GridR]

}
