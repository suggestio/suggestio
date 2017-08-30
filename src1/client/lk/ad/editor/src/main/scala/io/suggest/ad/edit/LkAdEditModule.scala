package io.suggest.ad.edit

import com.softwaremill.macwire._
import io.suggest.ad.edit.v.edit.strip.{PlusMinusControlsR, StripEditR}
import io.suggest.ad.edit.v.v.edit.text.TextEditR
import io.suggest.ad.edit.v.{LkAdEditCss, LkAdEditFormR}
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


  // css deps
  lazy val lkAdEditCss = wire[LkAdEditCss]


  // views deps
  lazy val plusMinusControlsR = wire[PlusMinusControlsR]

  lazy val stripEditR = wire[StripEditR]

  lazy val lkAdEditFormR = wire[LkAdEditFormR]

  lazy val textEditR = wire[TextEditR]


  // circuit deps
  lazy val lkAdEditCircuit = wire[LkAdEditCircuit]

}

