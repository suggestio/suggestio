package io.suggest.sjs.common.dt.interval

import io.suggest.common.menum.LightEnumeration
import io.suggest.dt.interval.{PeriodsConstants, QuickAdvPeriodsT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 19:41
  * Description: Sjs-модель периодов быстрого размещения.
  */
object QuickAdvPeriods extends QuickAdvPeriodsT with LightEnumeration {

  case class Val(override val strId: String) extends ValT

  override type T = Val

  override val P3D: T = Val( PeriodsConstants.P_3DAYS )
  override val P1W: T = Val( PeriodsConstants.P_1WEEK )
  override val P1M: T = Val( PeriodsConstants.P_1MONTH )
  val Custom      : T = Val( PeriodsConstants.CUSTOM )


  def values: List[T] = P3D :: P1W :: P1M :: Custom :: Nil

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case P3D.strId      => Some(P3D)
      case P1W.strId      => Some(P1W)
      case P1M.strId      => Some(P1M)
      case Custom.strId   => Some(Custom)
      case _              => None
    }
  }

}
