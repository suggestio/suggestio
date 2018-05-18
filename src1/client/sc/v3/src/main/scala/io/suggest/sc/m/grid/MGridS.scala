package io.suggest.sc.m.grid

import diode.FastEq
import io.suggest.common.geom.d2.MSize2di
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.18 15:55
  * Description: Состояние плитки, включающее в себя неотображаемые части плитки.
  */
object MGridS {

  implicit object MGridSFastEq extends FastEq[MGridS] {
    override def eqv(a: MGridS, b: MGridS): Boolean = {
      (a.core ===* b.core) &&
        (a.gridSz ===* b.gridSz) &&
        (a.hasMoreAds ==*  b.hasMoreAds)
    }
  }

  implicit def univEq: UnivEq[MGridS] = UnivEq.derive

}


/** Общее состояние плитки: данные в
  *
  * @param core Отображаемые данные плитки для рендера.
  * @param gridSz Рассчитанный размер плитки, когда известен.
  */
case class MGridS(
                   core         : MGridCoreS,
                   gridSz       : Option[MSize2di]      = None,
                   hasMoreAds   : Boolean               = true,
                 ) {

  def withCore(core: MGridCoreS) = copy(core = core)
  def withGridSz(gridSz: Option[MSize2di]) = copy(gridSz = gridSz)
  def withHasMoreAds(hasMoreAds: Boolean) = copy(hasMoreAds = hasMoreAds)

}
