package io.suggest.lk.m.img

import com.github.dominictobias.react.image.crop.{PercentCrop, PixelCrop}
import diode.FastEq
import io.suggest.img.crop.MCrop
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import io.suggest.ueq.ReactImageCropUnivEqUtil._
import monocle.macros.GenLens

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

  @inline implicit def univEq: UnivEq[MPictureCropPopup] = UnivEq.derive

  val origCrop      = GenLens[MPictureCropPopup](_.origCrop)
  val imgEdgeUid    = GenLens[MPictureCropPopup](_.imgEdgeUid)
  val percentCrop   = GenLens[MPictureCropPopup](_.percentCrop)
  val pixelCrop     = GenLens[MPictureCropPopup](_.pixelCrop)

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
                            )
