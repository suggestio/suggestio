package io.suggest.sc.m.menu

import diode.FastEq
import diode.data.Pot
import io.suggest.dev.MOsFamily
import io.suggest.sc.app.MScAppGetResp
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 8:40
  * Description: Модель состояния диалога доступа к скачиванию нативного приложения.
  * Неявно-пустая модель.
  */
object MDlAppDia {

  def empty = apply()

  implicit object MDlAppDiaFeq extends FastEq[MDlAppDia] {
    override def eqv(a: MDlAppDia, b: MDlAppDia): Boolean = {
      (a.opened ==* b.opened) &&
      (a.getReq ===* b.getReq) &&
      (a.platform ===* b.platform) &&
      (a.expanded ===* b.expanded)
    }
  }

  @inline implicit def univEq: UnivEq[MDlAppDia] = UnivEq.derive

  lazy val opened = GenLens[MDlAppDia](_.opened)
  lazy val getReq = GenLens[MDlAppDia](_.getReq)
  lazy val platform = GenLens[MDlAppDia](_.platform)
  lazy val expanded = GenLens[MDlAppDia](_.expanded)

}


/** Контейнер данных пункта меню скачивания мобильного приложения.
  *
  * @param opened Раскрыт ли пункт меню?
  * @param getReq Состояние реквеста к серверу за скачкой.
  * @param platform Текущая платформа/ось в диалоге выбора платформы.
  * @param expanded Индекс раскрытого пункта списка вариантов скачивания.
  */
case class MDlAppDia(
                      opened           : Boolean                  = false,
                      getReq           : Pot[MScAppGetResp]       = Pot.empty,
                      platform         : Option[MOsFamily]        = None,
                      expanded         : Option[Int]              = None,
                    )
