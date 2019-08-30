package io.suggest.sc.m.grid

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.18 15:55
  * Description: Состояние плитки, включающее в себя неотображаемые части плитки.
  */
object MGridS {

  implicit object MGridSFastEq extends FastEq[MGridS] {
    override def eqv(a: MGridS, b: MGridS): Boolean = {
      (a.core       ===* b.core) &&
      (a.hasMoreAds ==*  b.hasMoreAds)
    }
  }

  @inline implicit def univEq: UnivEq[MGridS] = UnivEq.derive

  val core = GenLens[MGridS](_.core)
  val hasMoreAds = GenLens[MGridS](_.hasMoreAds)

}


/** Общее состояние плитки: данные в
  *
  * @param core Отображаемые данные плитки для рендера.
  */
case class MGridS(
                   core         : MGridCoreS,
                   hasMoreAds   : Boolean               = true,
                 )
