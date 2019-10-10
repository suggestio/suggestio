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

  implicit object MJdRuntimeFastEq extends FastEq[MJdRuntime] {
    override def eqv(a: MJdRuntime, b: MJdRuntime): Boolean = {
      //(a.jdCss ===* b.jdCss) &&
      //MJdRuntimeData.MJdRuntimeDataFastEq.eqv( a.data, b.data )
      (a.gen ==* b.gen)
    }
  }

  @inline implicit def univEq: UnivEq[MJdRuntime] = UnivEq.derive

  val jdCss   = GenLens[MJdRuntime](_.jdCss)
  val data    = GenLens[MJdRuntime](_.data)
  val gen     = GenLens[MJdRuntime](_.gen)

}


/** Контейнер рантаймовых данных jd-рендера.
  *
  * @param jdCss Отрендеренный css.
  * @param data Пошаренный контейнер данных рантайма.
  * @param gen Счётчик поколения инстанса модели, чтобы управлять необходимостью пере-рендеривания плитки.
  *            Счётчик инкрементируется сам по умолчанию, но можно вручную управлять значением,
  *            которое непосредственно влияет на FastEq.eqv().
  */
case class MJdRuntime(
                       jdCss      : JdCss,
                       data       : MJdRuntimeData,
                       gen        : Long            = System.currentTimeMillis(),
                     )
