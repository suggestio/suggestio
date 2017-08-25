package io.suggest.jd.render

import com.softwaremill.macwire._
import io.suggest.jd.render.v.{JdCssFactory, JdCssR, JdR, JdRendererFactory}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 18:47
  * Description: Compile-time DI для модуля jd-render.
  */
class JdRenderModule {

  lazy val jdCssFactory = wire[JdCssFactory]

  lazy val jdCssR = wire[JdCssR]

  lazy val jdRenderFactory = wire[JdRendererFactory]

  lazy val jdR = wire[JdR]

}
