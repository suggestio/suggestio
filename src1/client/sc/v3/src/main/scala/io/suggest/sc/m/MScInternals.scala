package io.suggest.sc.m

import diode.FastEq
import io.suggest.sc.m.boot.MScBoot
import io.suggest.sc.m.jsrr.MJsRouterS
import io.suggest.sc.sc3.MSc3Conf
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.17 11:25
  * Description: Модель разных чисто-внутренних подсистем выдачи.
  * Сюда сваливается всё важное и не подходящее в иные модели.
  */
object MScInternals {

  implicit object MScInternalsFastEq extends FastEq[MScInternals] {
    override def eqv(a: MScInternals, b: MScInternals): Boolean = {
      (a.jsRouter ===* b.jsRouter) &&
      (a.geoLockTimer ===* b.geoLockTimer) &&
      (a.boot ===* b.boot) &&
      (a.conf ===* b.conf)
    }
  }

  @inline implicit def univEq: UnivEq[MScInternals] = UnivEq.force

  val conf      = GenLens[MScInternals](_.conf)
  val jsRouter  = GenLens[MScInternals](_.jsRouter)

  val boot      = GenLens[MScInternals](_.boot)

}


/** Класс-контейнер модели внутренних состояний.
  *
  * @param jsRouter Состояния js-роутера, который инициализируется асинхронно при загрузке выдачи.
  * @param geoLockTimer Сейчас происходит ожидание геолокации, это блокирует переключение выдачи (TailAh)
  */
case class MScInternals(
                         conf           : MSc3Conf,
                         jsRouter       : MJsRouterS          = MJsRouterS.empty,
                         geoLockTimer   : Option[Int]         = None,
                         boot           : MScBoot             = MScBoot.default,
                       ) {

  def withConf(conf: MSc3Conf) = copy(conf = conf)
  def withJsRouter(jsRouter: MJsRouterS)    = copy(jsRouter = jsRouter)
  def withGeoLockTimer(geoLockTimer: Option[Int])   = copy(geoLockTimer = geoLockTimer)
  def withBoot(boot: MScBoot) = copy(boot = boot)

}
