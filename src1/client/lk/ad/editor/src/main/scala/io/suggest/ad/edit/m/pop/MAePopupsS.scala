package io.suggest.ad.edit.m.pop

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.lk.m.img.MPictureCropPopup
import io.suggest.lk.m.{MDeleteConfirmPopupS, MErrorPopupS}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

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
        (a.pictureCrop ===* b.pictureCrop) &&
        (a.deleteConfirm ===* b.deleteConfirm)
    }
  }

  @inline implicit def univEq: UnivEq[MAePopupsS] = UnivEq.derive

  val error = GenLens[MAePopupsS](_.error)
  val pictureCrop = GenLens[MAePopupsS](_.pictureCrop)
  val deleteConfirm = GenLens[MAePopupsS](_.deleteConfirm)

}


/** Класс модели состояний попапов.
  *
  * @param error Состояние попапа с ошибками.
  * @param pictureCrop Состояние кропа картинки, если попап открыт.
  *                    None - без попапа.
  */
case class MAePopupsS(
                       error          : Option[MErrorPopupS]          = None,
                       pictureCrop    : Option[MPictureCropPopup]     = None,
                       deleteConfirm  : Option[MDeleteConfirmPopupS]  = None
                     )
  extends EmptyProduct
