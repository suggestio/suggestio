package io.suggest.ad.edit.m.pop

import com.github.dominictobias.react.image.crop.{PercentCrop, PixelCrop}
import diode.FastEq
import io.suggest.ueq.ReactImageCropUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 11:28
  * Description: Модель данных для попапа кропа изображения.
  */
object MPictureCropPopup {

  implicit object MPictureCropPopupFastEq extends FastEq[MPictureCropPopup] {
    override def eqv(a: MPictureCropPopup, b: MPictureCropPopup): Boolean = {
      (a.imgSrc ===* b.imgSrc) &&
        (a.percentCrop ===* b.percentCrop) &&
        (a.pixelCrop ===* b.pixelCrop)
    }
  }

  implicit def univEq: UnivEq[MPictureCropPopup] = UnivEq.derive

}


/** Состояние попапа кропа изображения.
  *
  * @param imgSrc Значение img.src
  * @param percentCrop Состояние кропа в % от размеров изображения.
  * @param pixelCrop Состояние кропа в пикселях.
  */
case class MPictureCropPopup(
                              imgSrc      : String,
                              percentCrop : PercentCrop,
                              pixelCrop   : Option[PixelCrop]  = None
                            ) {

  def withImgSrc(imgSrc: String)                    = copy(imgSrc = imgSrc)
  def withPercentCrop(percentCrop: PercentCrop)     = copy(percentCrop = percentCrop)
  def withPixelCrop(pixelCrop: Option[PixelCrop])   = copy(pixelCrop = pixelCrop)

}
