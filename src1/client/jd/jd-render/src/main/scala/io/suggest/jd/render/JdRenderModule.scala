package io.suggest.jd.render

import com.softwaremill.macwire._
import io.suggest.grid.GridModule
import io.suggest.jd.render.u.JdGridUtil
import io.suggest.jd.render.v._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 18:47
  * Description: Compile-time DI для модуля jd-render.
  */
object JdRenderModule {

  // deps
  val gridSjsModule = wire[GridModule]
  import gridSjsModule._


  // impl

  lazy val jdR = wire[JdR]

  lazy val jdGridUtil = wire[JdGridUtil]

  lazy val jdCssStatic = wire[JdCssStatic]

}
