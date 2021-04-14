package io.suggest.sc.m.grid

import io.suggest.common.empty.EmptyProductPot
import io.suggest.grid.build.MGridBuildResult
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.MJdRuntime
import io.suggest.jd.render.v.JdEventListener
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 21:55
  * Description: Модель данных состояния плитки, которые влияют на рендер.
  * Данные, не влияющие на отображение, не должны находится в этой модели.
  */
object MGridCoreS {

  @inline implicit def univEq: UnivEq[MGridCoreS] = UnivEq.derive


  def jdConf    = GenLens[MGridCoreS](_.jdConf)
  def jdRuntime = GenLens[MGridCoreS](_.jdRuntime)
  def ads       = GenLens[MGridCoreS](_.ads)
  def gridBuild = GenLens[MGridCoreS](_.gridBuild)

}


/** Класс модели состояния плитки карточек.
  *
  * @param ads Содержимое плитки.
  *            Pot реквеста к серверу за новыми карточками для плитки.
  * @param gridBuild Результат сборки плитки в контроллере.
  */
case class MGridCoreS(
                       jdConf         : MJdConf,
                       jdRuntime      : MJdRuntime,
                       ads            : MGridAds                      = MGridAds.empty,
                       gridBuild      : MGridBuildResult              = MGridBuildResult.empty,
                     )
  extends EmptyProductPot
