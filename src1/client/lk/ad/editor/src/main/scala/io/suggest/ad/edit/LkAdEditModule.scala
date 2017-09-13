package io.suggest.ad.edit

import com.softwaremill.macwire._
import diode.ModelRW
import io.suggest.ad.edit.c.DocEditAh
import io.suggest.ad.edit.m.{MAdEditRoot, MDocS}
import io.suggest.ad.edit.v.edit.AddR
import io.suggest.ad.edit.v.edit.strip.{DeleteStripBtnR, PlusMinusControlsR, StripEditR}
import io.suggest.ad.edit.v.{LkAdEditCss, LkAdEditFormR}
import io.suggest.jd.render.JdRenderModule
import io.suggest.quill.QuillSioModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 18:56
  * Description: Compile-time DI для редактора карточек.
  */
class LkAdEditModule {

  lazy val jdRenderModule = wire[JdRenderModule]

  lazy val quillSioModule = wire[QuillSioModule]


  import jdRenderModule._
  import quillSioModule._


  // css deps
  lazy val lkAdEditCss = wire[LkAdEditCss]


  // views deps
  lazy val plusMinusControlsR = wire[PlusMinusControlsR]

  lazy val stripEditR = wire[StripEditR]

  lazy val lkAdEditFormR = wire[LkAdEditFormR]

  lazy val deleteStripBtnR = wire[DeleteStripBtnR]

  lazy val addR = wire[AddR]


  // circuit deps
  lazy val lkAdEditCircuit = wire[LkAdEditCircuit]

  def docEditAhFactory = (modelRW: ModelRW[MAdEditRoot, MDocS]) => wire[DocEditAh[MAdEditRoot]]

}

