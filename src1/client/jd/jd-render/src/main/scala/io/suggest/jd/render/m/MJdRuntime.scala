package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.render.v.JdCss
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.07.19 18:54
  * Description: Модель рантаймовых данных, которые целиком пакетно перегенеривается
  * при изменении плитки (шаблонов, jd-конфига и т.д.).
  */
object MJdRuntime {

  @inline implicit def univEq: UnivEq[MJdRuntime] = UnivEq.derive

  val jdCss   = GenLens[MJdRuntime](_.jdCss)
  val data    = GenLens[MJdRuntime](_.data)

}


/** Контейнер рантаймовых данных jd-рендера.
  *
  * @param jdCss Отрендеренный css.
  * @param data Пошаренный контейнер данных рантайма.
  */
case class MJdRuntime(
                       jdCss      : JdCss,
                       data       : MJdRuntimeData,
                     )
