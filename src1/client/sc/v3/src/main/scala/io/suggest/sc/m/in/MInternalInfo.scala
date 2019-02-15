package io.suggest.sc.m.in

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import io.suggest.sc.sc3.Sc3Pages.MainScreen
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.19 11:21
  * Description: Контейнер данных для различных internal-полей.
  */
object MInternalInfo {

  def empty = apply()

  implicit object MInternalInfoFastEq extends FastEq[MInternalInfo] {
    override def eqv(a: MInternalInfo, b: MInternalInfo): Boolean = {
      (a.geoLockTimer ===* b.geoLockTimer) &&
      (a.currRoute ===* b.currRoute)
    }
  }

  @inline implicit def univEq: UnivEq[MInternalInfo] = UnivEq.derive

  val geoLockTimer  = GenLens[MInternalInfo](_.geoLockTimer)
  val currRoute     = GenLens[MInternalInfo](_.currRoute)

}


/** Контейнер различных внутренних данных.
  *
  * @param geoLockTimer Таймер ожидания геолокации.
  * @param currRoute Текущая роута.
  */
case class MInternalInfo(
                          geoLockTimer   : Option[Int]         = None,
                          currRoute      : Option[MainScreen]  = None,
                        )
