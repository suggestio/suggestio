package io.suggest.id.login.m

import diode.FastEq
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.id.login.m.reg.{MEpwRegS, MRegFinishS}
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
      (a.ext ===* b.ext) &&
      (a.overall ===* b.overall) &&
      (a.epwReg ===* b.epwReg)
    }
  }

  @inline implicit def univEq: UnivEq[MLoginRootS] = UnivEq.derive

  val epw           = GenLens[MLoginRootS]( _.epw )
  val ext           = GenLens[MLoginRootS]( _.ext )
  val overall       = GenLens[MLoginRootS]( _.overall )
  val epwReg        = GenLens[MLoginRootS]( _.epwReg )

}


/** Корневой контейнер формы логина.
  *
  * @param epw Состояние формы логина по имени-email и пароля.
  * @param ext Состояние логина через внешний сервис.
  * @param overall Общее состояние формы.
  */
case class MLoginRootS(
                        epw         : MEpwLoginS            = MEpwLoginS(),
                        ext         : MExtLoginFormS        = MExtLoginFormS(),
                        overall     : MLoginFormOverallS    = MLoginFormOverallS(),
                        epwReg      : MEpwRegS              = MEpwRegS(),
                        regFinish   : MRegFinishS           = MRegFinishS(),
                      )
