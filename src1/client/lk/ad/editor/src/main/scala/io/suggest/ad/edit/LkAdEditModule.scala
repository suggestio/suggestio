package io.suggest.ad.edit

import com.softwaremill.macwire._
import diode.ModelRW
import io.suggest.ad.edit.c.DocEditAh
import io.suggest.ad.edit.m.MAeRoot
import io.suggest.ad.edit.m.edit.MDocS
import io.suggest.ad.edit.v.edit._
import io.suggest.ad.edit.v.edit.content.{ContentEditCssR, ContentLayerBtnR, ContentLayersR}
import io.suggest.ad.edit.v.edit.strip._
import io.suggest.ad.edit.v.pop.LaePopupsR
import io.suggest.ad.edit.v.{LkAdEditCss, LkAdEditFormR}
import io.suggest.jd.edit.JdEditModule
import io.suggest.lk.r.SlideBlockR
import io.suggest.quill.QuillSioModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 18:56
  * Description: Compile-time DI для редактора карточек.
  */
class LkAdEditModule {

  final type M = MAeRoot

  import io.suggest.jd.render.JdRenderModule._
  val jdEditModule = wire[JdEditModule]
  val quillSioModule = wire[QuillSioModule]


  import io.suggest.lk.LkCommonModule._
  import quillSioModule._
  import jdEditModule._

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

  lazy val sliderOptR = wire[SliderOptR]
  lazy val rotateR = wire[RotateR]
  lazy val widthPxOptR = wire[WidthPxOptR]

  lazy val contentEditCssR = wire[ContentEditCssR]


  lazy val contentLayerR = wire[ContentLayersR]
  lazy val contentLayerBtnR = wire[ContentLayerBtnR]


  // circuit deps

  def docEditAhFactory = (modelRW: ModelRW[M, MDocS]) => wire[DocEditAh[M]]

  lazy val lkAdEditCircuit = wire[LkAdEditCircuit]

}

