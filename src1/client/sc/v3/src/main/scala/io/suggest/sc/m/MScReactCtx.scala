package io.suggest.sc.m

import diode.FastEq
import io.suggest.sc.v.styl.{ScCss, ScCssSemiStatic}
import io.suggest.spa.SioPages
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.02.19 12:53
  * Description: Контекст рендера шаблонов (react-компонентов) выдачи.
  * Пробрасывается через react-context API
  */
object MScReactCtx {

  implicit object MScReactCtxFastEq extends FastEq[MScReactCtx] {
    override def eqv(a: MScReactCtx, b: MScReactCtx): Boolean = {
      // Тут проверка, не изменился ли контекст.
      // scCss - меняется, но id классов всегда стабильны by-design, поэтому его не надо передёргивать.
      // routerCtl - инстанс создаётся при запуске и больше никогда не меняется.
      // Messages, если он тут будет (а не в другом контексте), можно проверять через eq.
      true
    }
  }

  @inline implicit def univEq: UnivEq[MScReactCtx] = UnivEq.force

}


/** Контейнер данных sc-контекста react.
  *
  * @see [[https://reactjs.org/docs/context.html]]
  * @param getScCss ScCss.
  * @param routerCtl Контроллер роутера, пробрасываемый вниз по цепочке.
  */
case class MScReactCtx(
                        getScCss          : () => ScCss,
                        routerCtl         : RouterCtl[SioPages],
                        scCssSemiStatic   : ScCssSemiStatic,
                      ) {

  def scCss: ScCss = getScCss()

}
