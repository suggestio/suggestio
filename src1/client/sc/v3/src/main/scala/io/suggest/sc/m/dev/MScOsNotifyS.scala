package io.suggest.sc.m.dev

import diode.FastEq
import io.suggest.os.notify.api.cnl.MCnlNotifierS
import io.suggest.os.notify.api.html5.MHtml5NotifyAdpS
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.common.empty.OptionUtil.BoolOptOps

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
    def hasPermission: Boolean = {
      osn.cnl
        .map(_.permission)
        .orElse(osn.html5.map(_.permission))
        .flatten
        .getOrElseFalse
    }
  }

}


/** Контейнер состояний различных адаптеров нотификации.
  *
  * @param cnl Состояние адаптера cordova-plugin-local-notification.
  */
case class MScOsNotifyS(
                         cnl          : Option[MCnlNotifierS]       = None,
                         html5        : Option[MHtml5NotifyAdpS]    = None,
                       )
