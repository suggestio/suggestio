package io.suggest.ad.edit.m.pop

import com.github.dominictobias.react.image.crop.{PercentCrop, PixelCrop}
import diode.FastEq
import io.suggest.img.crop.MCrop
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.ueq.ReactImageCropUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 11:28
  * Description: Модель данных для попапа кропа изображения.
  */
object MPictureCropPopup {

  implicit object MPictureCropPopupFastEq extends FastEq[MPictureCropPopup] {
    override def eqv(a: MPictureCropPopup, b: MPictureCropPopup): Boolean = {
      (a.origCrop ===* b.origCrop) &&
      (a.imgEdgeUid ==* b.imgEdgeUid) &&
        (a.percentCrop ===* b.percentCrop) &&
        (a.pixelCrop ===* b.pixelCrop)
    }
  }

  implicit def univEq: UnivEq[MPictureCropPopup] = UnivEq.derive

}


/** Состояние попапа кропа изображения.
  *
  * @param origCrop Исходный кроп, чтобы при cancel можно было откатиться назад.
  * @param imgEdgeUid id эджа по карте эжей
  * @param percentCrop Состояние кропа в % от размеров изображения.
  * @param pixelCrop Состояние кропа в пикселях.
  */
case class MPictureCropPopup(
                              origCrop    : Option[MCrop],
                              imgEdgeUid  : EdgeUid_t,
                              percentCrop : PercentCrop,
                              pixelCrop   : Option[PixelCrop]  = None
                            ) {

  def withEdgeUid(imgEdgeUid: EdgeUid_t)            = copy(imgEdgeUid = imgEdgeUid)
  def withPercentCrop(percentCrop: PercentCrop)     = copy(percentCrop = percentCrop)
  def withPixelCrop(pixelCrop: Option[PixelCrop])   = copy(pixelCrop = pixelCrop)

}
