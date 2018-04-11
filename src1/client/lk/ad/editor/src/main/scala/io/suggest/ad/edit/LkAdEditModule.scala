package io.suggest.ad.edit

import com.softwaremill.macwire._
import diode.ModelRW
import io.suggest.ad.edit.c.{ColorPickAh, DocEditAh}
import io.suggest.ad.edit.m.edit.color.MColorPick
import io.suggest.ad.edit.m.{MAeRoot, MDocS}
import io.suggest.ad.edit.v.edit._
import io.suggest.ad.edit.v.edit.color.ColorCheckboxR
import io.suggest.ad.edit.v.edit.strip._
import io.suggest.ad.edit.v.pop.LaePopupsR
import io.suggest.ad.edit.v.{LkAdEditCss, LkAdEditFormR}
import io.suggest.jd.render.JdRenderModule
import io.suggest.lk.LkCommonModule
import io.suggest.lk.r.SlideBlockR
import io.suggest.lk.r.color.ColorPickerR
import io.suggest.lk.r.crop.CropPopupR
import io.suggest.quill.QuillSioModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 18:56
  * Description: Compile-time DI для редактора карточек.
  */
class LkAdEditModule {

  final type M = MAeRoot

  val jdRenderModule = wire[JdRenderModule]

  val quillSioModule = wire[QuillSioModule]

  val lkCommonModule = wire[LkCommonModule]

  import lkCommonModule._
  import jdRenderModule._
  import quillSioModule._

  // srv deps


  // css deps
  lazy val lkAdEditCss = wire[LkAdEditCss]


  // views deps
  lazy val plusMinusControlsR = wire[PlusMinusControlsR]

  lazy val lkAdEditFormR = wire[LkAdEditFormR]

  lazy val deleteStripBtnR = wire[DeleteStripBtnR]

  lazy val addR = wire[AddR]

  lazy val colorCheckboxR = wire[ColorCheckboxR]

  lazy val colorPickerR = wire[ColorPickerR]

  lazy val scaleR = wire[ScaleR]

  lazy val saveR = wire[SaveR]

  lazy val showWideR = wire[ShowWideR]

  lazy val useAsMainR = wire[UseAsMainR]

  lazy val deleteBtnR = wire[DeleteBtnR]


  lazy val cropPopupR = wire[CropPopupR]

  lazy val laePopupsR = wire[LaePopupsR]

  lazy val slideBlockR = wire[SlideBlockR]

  lazy val rotateR = wire[RotateR]


  // circuit deps

  def docEditAhFactory = (modelRW: ModelRW[M, MDocS]) => wire[DocEditAh[M]]

  def colorPickAh = (modelRW: ModelRW[M, Option[MColorPick]]) => wire[ColorPickAh[M]]

  lazy val lkAdEditCircuit = wire[LkAdEditCircuit]

}

