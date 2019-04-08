package io.suggest.sc.m.in

import diode.FastEq
import io.suggest.sc.m.boot.MScBoot
import io.suggest.sc.sc3.MSc3Conf
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

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
      (a.info ===* b.info) &&
      (a.jsRouter ===* b.jsRouter) &&
      (a.boot ===* b.boot) &&
      (a.conf ===* b.conf)
    }
  }

  @inline implicit def univEq: UnivEq[MScInternals] = UnivEq.force

  val info          = GenLens[MScInternals](_.info)
  val conf          = GenLens[MScInternals](_.conf)
  val jsRouter      = GenLens[MScInternals](_.jsRouter)
  val boot          = GenLens[MScInternals](_.boot)

}


/** Класс-контейнер модели внутренних состояний.
  *
  * @param routing Состояния js-роутера, который инициализируется асинхронно при загрузке выдачи.
  * @param geoLockTimer Сейчас происходит ожидание геолокации, это блокирует переключение выдачи (TailAh)
  */
case class MScInternals(
                         info           : MInternalInfo       = MInternalInfo.empty,
                         conf           : MSc3Conf,
                         jsRouter       : MJsRouterS          = MJsRouterS.empty,
                         boot           : MScBoot             = MScBoot.default,
                       )