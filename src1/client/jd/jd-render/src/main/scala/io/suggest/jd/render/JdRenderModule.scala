package io.suggest.jd.render

import com.softwaremill.macwire._
import io.suggest.grid.GridModule
import io.suggest.jd.render.v._
import io.suggest.lk.LkCommonModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 18:47
  * Description: Compile-time DI для модуля jd-render.
  */
class JdRenderModule {

  // deps
  val gridSjsModule = wire[GridModule]
  import gridSjsModule._

  val lkCommonModule = wire[LkCommonModule]
  import lkCommonModule._


  // impl

  lazy val jdCssR = wire[JdCssR]

  lazy val jdR = wire[JdR]

  lazy val jdGridUtil = wire[JdGridUtil]

}
