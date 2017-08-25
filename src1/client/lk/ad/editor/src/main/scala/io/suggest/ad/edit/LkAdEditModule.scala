package io.suggest.ad.edit

import com.softwaremill.macwire._
import io.suggest.ad.edit.v.LkAdEditFormR
import io.suggest.jd.render.JdRenderModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 18:56
  * Description: Compile-time DI для редактора карточек.
  */
class LkAdEditModule {

  lazy val jdRenderModule = wire[JdRenderModule]

  import jdRenderModule._


  lazy val lkAdEditFormR = wire[LkAdEditFormR]

  lazy val lkAdEditCircuit = wire[LkAdEditCircuit]

}
