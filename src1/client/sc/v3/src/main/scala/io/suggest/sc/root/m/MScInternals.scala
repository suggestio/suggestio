package io.suggest.sc.root.m

import diode.FastEq
import diode.data.Pot
import io.suggest.routes.scRoutes
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.17 11:25
  * Description: Модель разных чисто-внутренних подсистем выдачи.
  * Сюда сваливается всё важное и не подходящее в иные модели.
  */
object MScInternals {

  def empty = apply()

  implicit object MScInternalsFastEq extends FastEq[MScInternals] {
    override def eqv(a: MScInternals, b: MScInternals): Boolean = {
      a.jsRouter eq b.jsRouter
    }
  }

  implicit def univEq: UnivEq[MScInternals] = UnivEq.force

}


/** Класс-контейнер модели внутренних состояний.
  *
  * @param jsRouter Состояния js-роутера, который инициализируется асинхронно при загрузке выдачи.
  */
case class MScInternals(
                         jsRouter      : Pot[scRoutes.type]      = Pot.empty
                       ) {

  def withJsRouter(jsRouter: Pot[scRoutes.type]) = copy(jsRouter = jsRouter)

}
