package io.suggest.sc.m.grid

import diode.FastEq
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
        (a.hasMoreAds ==*  b.hasMoreAds)
    }
  }

  @inline implicit def univEq: UnivEq[MGridS] = UnivEq.derive

}


/** Общее состояние плитки: данные в
  *
  * @param core Отображаемые данные плитки для рендера.
  * @param gridSz Рассчитанный размер плитки, когда известен.
  */
case class MGridS(
                   core         : MGridCoreS,
                   hasMoreAds   : Boolean               = true,
                 ) {

  def withCore(core: MGridCoreS) = copy(core = core)
  def withHasMoreAds(hasMoreAds: Boolean) = copy(hasMoreAds = hasMoreAds)

}
