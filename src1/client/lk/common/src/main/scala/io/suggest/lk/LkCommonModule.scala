package io.suggest.lk

import com.softwaremill.macwire._
import io.suggest.lk.r.{SaveR, UploadStatusR}
import io.suggest.lk.r.color.{ColorBtnR, ColorPickerR, ColorsSuggestR}
import io.suggest.lk.r.crop.{CropBtnR, CropPopupR}
import io.suggest.lk.r.img.ImgEditBtnR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.18 21:47
  */
class LkCommonModule {

  lazy val colorPickerR = wire[ColorPickerR]

  lazy val colorBtnR = wire[ColorBtnR]

  lazy val colorSuggestR = wire[ColorsSuggestR]

  lazy val uploadStatusR = wire[UploadStatusR]

  lazy val imgEditBtnR = wire[ImgEditBtnR]

  lazy val cropBtnR = wire[CropBtnR]

  lazy val cropPopupR = wire[CropPopupR]

  lazy val saveR = wire[SaveR]

}
