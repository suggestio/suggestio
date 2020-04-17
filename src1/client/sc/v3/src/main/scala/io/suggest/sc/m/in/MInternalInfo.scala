package io.suggest.sc.m.in

import diode.FastEq
import io.suggest.i18n.MCommonReactCtx
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
      (a.currRoute ===* b.currRoute) &&
      (a.commonReactCtx ===* b.commonReactCtx)
    }
  }

  @inline implicit def univEq: UnivEq[MInternalInfo] = UnivEq.derive

  val geoLockTimer  = GenLens[MInternalInfo]( _.geoLockTimer )
  val currRoute     = GenLens[MInternalInfo]( _.currRoute )
  val messages      = GenLens[MInternalInfo]( _.commonReactCtx )

}


/** Контейнер различных внутренних данных.
  *
  * @param geoLockTimer Таймер ожидания геолокации.
  * @param currRoute Текущая роута.
  * @param commonReactCtx Инстанс с сообщениями.
  */
final case class MInternalInfo(
                                geoLockTimer      : Option[Int]             = None,
                                currRoute         : Option[MainScreen]      = None,
                                commonReactCtx    : MCommonReactCtx         = MCommonReactCtx.default,
                              )
