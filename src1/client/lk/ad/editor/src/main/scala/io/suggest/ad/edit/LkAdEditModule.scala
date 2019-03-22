package io.suggest.ad.edit

import com.softwaremill.macwire._
import diode.ModelRW
import io.suggest.ad.edit.c.DocEditAh
import io.suggest.ad.edit.m.{MAeRoot, MDocS}
import io.suggest.ad.edit.v.edit._
import io.suggest.ad.edit.v.edit.content.{ContentEditCssR, ContentLayerBtnR, ContentLayersR}
import io.suggest.ad.edit.v.edit.strip._
import io.suggest.ad.edit.v.pop.LaePopupsR
import io.suggest.ad.edit.v.{LkAdEditCss, LkAdEditFormR}
import io.suggest.jd.render.JdRenderModule
import io.suggest.lk.LkCommonModule
import io.suggest.lk.m.color.MColorPick
import io.suggest.lk.r.SlideBlockR
import io.suggest.lk.r.color.ColorCheckBoxR
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


  import io.suggest.lk.LkCommonModule._
  import jdRenderModule._
  import quillSioModule._

  // srv deps


  // css deps
  lazy val lkAdEditCss = wire[LkAdEditCss]

  // lk-common: линкуем слайд-блок, т.к. SlideBlockCss собран в LkAdEditCss:
  lazy val slideBlockR = wire[SlideBlockR]


  // views deps
  lazy val plusMinusControlsR = wire[PlusMinusControlsR]

  lazy val lkAdEditFormR = wire[LkAdEditFormR]

  lazy val deleteStripBtnR = wire[DeleteStripBtnR]

  lazy val addR = wire[AddR]

  lazy val scaleR = wire[ScaleR]

  lazy val showWideR = wire[ShowWideR]

  lazy val useAsMainR = wire[UseAsMainR]

  lazy val deleteBtnR = wire[DeleteBtnR]

  lazy val textShadowR = wire[TextShadowR]


  lazy val laePopupsR = wire[LaePopupsR]

  lazy val rotateR = wire[RotateR]

  lazy val contentEditCssR = wire[ContentEditCssR]


  lazy val contentLayerR = wire[ContentLayersR]
  lazy val contentLayerBtnR = wire[ContentLayerBtnR]


  // circuit deps

  def docEditAhFactory = (modelRW: ModelRW[M, MDocS]) => wire[DocEditAh[M]]

  lazy val lkAdEditCircuit = wire[LkAdEditCircuit]

}

