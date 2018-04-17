package io.suggest.adn.edit.m

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.lk.m.MErrorPopupS
import io.suggest.lk.m.img.MPictureCropPopup
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.18 22:24
  * Description: Контейнер состояния попапов.
  */
object MAdnEditPopups {

  def empty = apply()

  implicit object MAdnEditPopupsFastEq extends FastEq[MAdnEditPopups] {
    override def eqv(a: MAdnEditPopups, b: MAdnEditPopups): Boolean = {
      (a.errorPopup ===* b.errorPopup) &&
        (a.cropPopup ===* b.cropPopup)
    }
  }

  implicit def univEq: UnivEq[MAdnEditPopups] = UnivEq.derive

}


case class MAdnEditPopups(
                           errorPopup     : Option[MErrorPopupS]        = None,
                           cropPopup      : Option[MPictureCropPopup]   = None,
                         )
  extends EmptyProduct
{

  def withErrorPopup(errorPopup: Option[MErrorPopupS])        = copy(errorPopup = errorPopup)
  def withCropPopup(cropPopup: Option[MPictureCropPopup])     = copy(cropPopup = cropPopup)

}
