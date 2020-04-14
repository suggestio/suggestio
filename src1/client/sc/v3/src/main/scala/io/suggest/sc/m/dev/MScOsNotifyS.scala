package io.suggest.sc.m.dev

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProduct
import io.suggest.os.notify.api.cnl.MCnlNotifierS
import io.suggest.os.notify.api.html5.MH5nAdpS
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.2020 16:00
  * Description: Состояние нотификаций и её адаптеров.
  */
object MScOsNotifyS {

  def empty = apply()

  implicit object MScOsNotifyFeq extends FastEq[MScOsNotifyS] {
    override def eqv(a: MScOsNotifyS, b: MScOsNotifyS): Boolean = {
      (a.cnl ===* b.cnl) &&
      (a.html5 ===* b.html5)
    }
  }

  @inline implicit def univEq: UnivEq[MScOsNotifyS] = UnivEq.derive


  lazy val cnl = GenLens[MScOsNotifyS](_.cnl)
  lazy val html5 = GenLens[MScOsNotifyS](_.html5)


  implicit final class ScOsNotifyOpsExt( private val osn: MScOsNotifyS ) extends AnyVal {

    def hasPermission: Pot[Boolean] = {
      osn.cnl.permission
        .orElse( osn.html5.permission )
    }

  }

}


/** Контейнер состояний различных адаптеров нотификации.
  *
  * @param cnl Состояние адаптера cordova-plugin-local-notification.
  * @param html5 Состояние адаптера html5-нотификаций.
  */
case class MScOsNotifyS(
                         cnl          : MCnlNotifierS               = MCnlNotifierS.empty,
                         html5        : MH5nAdpS                    = MH5nAdpS.empty,
                       )
  extends EmptyProduct
