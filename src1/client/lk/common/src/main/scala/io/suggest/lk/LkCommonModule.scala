package io.suggest.lk

import com.softwaremill.macwire._
import io.suggest.lk.r._
import io.suggest.lk.r.captcha.CaptchaFormR
import io.suggest.lk.r.color._
import io.suggest.lk.r.img.{CropBtnR, CropPopupR, ImgEditBtnR, LkImgUtilJs}
import io.suggest.lk.r.sms.SmsCodeFormR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.18 21:47
  */
object LkCommonModule {

  import io.suggest.ReactCommonModule._

  lazy val colorPickerR = wire[ColorPickerR]

  lazy val colorBtnR = wire[ColorBtnR]

  lazy val colorSuggestR = wire[ColorsSuggestR]

  lazy val colorCheckboxR = wire[ColorCheckBoxR]

  lazy val uploadStatusR = wire[UploadStatusR]

  lazy val imgEditBtnR = wire[ImgEditBtnR]


  lazy val cropRenderUtilJs = wire[LkImgUtilJs]

  lazy val cropBtnR = wire[CropBtnR]

  lazy val cropPopupR = wire[CropPopupR]

  lazy val lkCss = wire[LkCss]

  lazy val inputSliderR = wire[InputSliderR]

  lazy val lkCheckBoxR = wire[LkCheckBoxR]

  lazy val sliderOptR = wire[SliderOptR]


  lazy val saveR = wire[SaveR]


  lazy val captchaFormR = wire[CaptchaFormR]

  lazy val smsCodeFormR = wire[SmsCodeFormR]

  lazy val filesDropZoneR = wire[FilesDropZoneR]

  lazy val touchSwitchR = wire[TouchSwitchR]

}
