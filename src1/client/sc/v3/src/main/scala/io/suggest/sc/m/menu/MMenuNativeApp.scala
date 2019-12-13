package io.suggest.sc.m.menu

import diode.FastEq
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 8:40
  * Description: Модель состояния разделам менюшки скачивания нативного приложения.
  * Модель опциональная, т.к. внутри мобильного приложения этот пункт не отображается.
  */
object MMenuNativeApp {

  def empty = apply()

  implicit object MMenuNativeAppDlFastEq extends FastEq[MMenuNativeApp] {
    override def eqv(a: MMenuNativeApp, b: MMenuNativeApp): Boolean = {
      (a.opened ==* b.opened)
    }
  }

  @inline implicit def univEq: UnivEq[MMenuNativeApp] = UnivEq.derive

  val opened = GenLens[MMenuNativeApp](_.opened)

}


/** Контейнер данных пункта меню скачивания мобильного приложения.
  *
  * @param opened Раскрыт ли пункт меню?
  */
case class MMenuNativeApp(
                           opened               : Boolean               = false,
                         )
