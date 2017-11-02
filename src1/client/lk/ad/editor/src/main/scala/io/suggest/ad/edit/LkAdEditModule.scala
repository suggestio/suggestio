package io.suggest.ad.edit

import com.softwaremill.macwire._
import diode.ModelRW
import io.suggest.ad.edit.c.{ColorPickAh, DocEditAh}
import io.suggest.ad.edit.m.edit.color.MColorPick
import io.suggest.ad.edit.m.{MAeRoot, MDocS}
import io.suggest.ad.edit.v.edit._
import io.suggest.ad.edit.v.edit.strip._
import io.suggest.ad.edit.v.pop.{LaePopupsR, PictureCropPopupR}
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

  final type M = MAeRoot

  lazy val jdRenderModule = wire[JdRenderModule]

  lazy val quillSioModule = wire[QuillSioModule]


  import jdRenderModule._
  import quillSioModule._

  // srv deps


  // css deps
  lazy val lkAdEditCss = wire[LkAdEditCss]


  // views deps
  lazy val plusMinusControlsR = wire[PlusMinusControlsR]

  lazy val stripEditR = wire[StripEditR]

  lazy val qdEditR = wire[QdEditR]

  lazy val lkAdEditFormR = wire[LkAdEditFormR]

  lazy val deleteStripBtnR = wire[DeleteStripBtnR]

  lazy val addR = wire[AddR]

  lazy val colorPickR = wire[ColorPickR]

  lazy val colorSuggestR = wire[ColorsSuggestR]

  lazy val pictureR = wire[PictureR]

  lazy val scaleR = wire[ScaleR]

  lazy val saveR = wire[SaveR]

  lazy val showWideR = wire[ShowWideR]

  lazy val useAsMainR = wire[UseAsMainR]

  lazy val deleteBtnR = wire[DeleteBtnR]


  lazy val pictureCropPopupR = wire[PictureCropPopupR]

  lazy val laePopupsR = wire[LaePopupsR]


  // circuit deps

  def docEditAhFactory = (modelRW: ModelRW[M, MDocS]) => wire[DocEditAh[M]]

  def colorPickAh = (modelRW: ModelRW[M, Option[MColorPick]]) => wire[ColorPickAh[M]]

  lazy val lkAdEditCircuit = wire[LkAdEditCircuit]

}

