package io.suggest.sc.m

import diode.FastEq
import diode.data.Pot
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.sc3.MSc3Conf
import japgolly.univeq._

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
      (a.jsRouter eq b.jsRouter) &&
        (a.geoLockTimer ==* b.geoLockTimer)
    }
  }

  implicit def univEq: UnivEq[MScInternals] = UnivEq.force

}


/** Класс-контейнер модели внутренних состояний.
  *
  * @param jsRouter Состояния js-роутера, который инициализируется асинхронно при загрузке выдачи.
  * @param geoLockTimer Сейчас происходит ожидание геолокации, это блокирует переключение выдачи (TailAh)
  */
case class MScInternals(
                         conf          : MSc3Conf,
                         jsRouter      : Pot[ScJsRoutes.type]    = Pot.empty,
                         geoLockTimer  : Option[Int]             = None,
                       ) {

  def withJsRouter(jsRouter: Pot[ScJsRoutes.type])    = copy(jsRouter = jsRouter)
  def withGeoLockTimer(geoLockTimer: Option[Int])   = copy(geoLockTimer = geoLockTimer)

  override def toString: String = {
    import io.suggest.common.html.HtmlConstants._
    new StringBuilder(128)
      .append( productPrefix )
      .append( `(` )
      .append( conf ).append( COMMA )
      .append( jsRouter.map(_ => "") ).append( COMMA )
      .append( geoLockTimer )
      .append( `)` )
      .toString()
  }

}
