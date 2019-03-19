package io.suggest.id.login.m

import diode.FastEq
import io.suggest.id.login.m.epw.MEpwLoginS
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 15:15
  * Description: Корневая модель состояния формы логина.
  */
object MLoginRootS {

  implicit object MLoginRootSFastEq extends FastEq[MLoginRootS] {
    override def eqv(a: MLoginRootS, b: MLoginRootS): Boolean = {
      (a.epw ===* b.epw) &&
      (a.overall ===* b.overall)
    }
  }

  @inline implicit def univEq: UnivEq[MLoginRootS] = UnivEq.derive

  val epw           = GenLens[MLoginRootS](_.epw)
  val overall       = GenLens[MLoginRootS](_.overall)

}


/** Корневой контейнер формы логина.
  *
  * @param epw Состояние формы логина по имени-email и пароля.
  * @param isVisible Видимый ли диалог?
  */
case class MLoginRootS(
                        epw         : MEpwLoginS            = MEpwLoginS(),
                        overall     : MLoginFormOverallS    = MLoginFormOverallS(),
                      )
