package io.suggest.jd.render

import com.softwaremill.macwire._
import diode.react.ModelProxy
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 18:47
  * Description: Compile-time DI для модуля jd-render.
  */
class JdRenderModule {

  lazy val jdCssFactory = wire[JdCssFactory]

  lazy val jdCssR = wire[JdCssR]

  lazy val jdRendererF = (jdArgsProxy: ModelProxy[MJdArgs]) => wire[JdRendererR]

  lazy val jdR = wire[JdR]

}
