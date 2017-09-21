package io.suggest.ad.edit.m.pop

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.lk.m.MErrorPopupS
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 11:50
  * Description: Над-модель состояний различных попапов.
  */

object MAePopupsS {

  def empty = apply()

  implicit object MAePopupsSFastEq extends FastEq[MAePopupsS] {
    override def eqv(a: MAePopupsS, b: MAePopupsS): Boolean = {
      (a.error ===* b.error) &&
        (a.pictureCrop ===* b.pictureCrop)
    }
  }

  implicit def univEq: UnivEq[MAePopupsS] = UnivEq.derive

}


/** Класс модели состояний попапов.
  *
  * @param error Состояние попапа с ошибками.
  * @param pictureCrop Состояние кропа картинки, если попап открыт.
  *                    None - без попапа.
  */
case class MAePopupsS(
                       error          : Option[MErrorPopupS]          = None,
                       pictureCrop    : Option[MPictureCropPopup]     = None
                     )
  extends EmptyProduct
{

  def withErrors(errors: Option[MErrorPopupS]) = copy(error = error)
  def withPictureCrop(pictureCrop: Option[MPictureCropPopup]) = copy(pictureCrop = pictureCrop)

}
