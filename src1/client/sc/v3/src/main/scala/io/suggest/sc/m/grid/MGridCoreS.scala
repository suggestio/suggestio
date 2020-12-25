package io.suggest.sc.m.grid

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProductPot
import io.suggest.grid.build.MGridBuildResult
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.{MJdDataJs, MJdRuntime}
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
  *
  * Неявно-пустая модель.
  */
object MGridCoreS {

  /** Поддержка FastEq для [[MGridCoreSFastEq]]. */
  implicit object MGridCoreSFastEq extends FastEq[MGridCoreS] {
    override def eqv(a: MGridCoreS, b: MGridCoreS): Boolean = {
      (a.jdConf     ==*  b.jdConf) &&
      (a.jdRuntime  ===* b.jdRuntime) &&
      (a.ads        ===* b.ads) &&
      (a.gridBuild  ===* b.gridBuild)
    }
  }

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
                       ads            : Pot[Vector[MScAdData]]        = Pot.empty,
                       gridBuild      : MGridBuildResult              = MGridBuildResult.empty,
                     )
  extends EmptyProductPot
{

  /** Текущая открытая карточка, если есть. */
  lazy val focusedAdOpt: Option[MScAdData] = {
    (for {
      adsVec <- ads.iterator
      emptyPot = Pot.empty[MJdDataJs]
      scAd <- adsVec.iterator
      if scAd.focused !=* emptyPot
    } yield {
      scAd
    })
      .nextOption()
  }

  /** Текущая открытая карточка, пригодная для операций над ней: размещение в маячке, например. */
  def myFocusedAdOpt: Option[MScAdData] = {
    focusedAdOpt.filter { scAd =>
      scAd.focusedOrMainPot.exists { foc =>
        (foc.info.canEditOpt contains[Boolean] true) &&
        // Запрещаем 404-карточки считать за "свои".
        !foc.info.isMad404
      }
    }
  }

  /** Происходит ли сейчас загрузка какой-либо карточки? */
  lazy val _adsHasPending: Boolean =
    ads.iterator.flatten.exists(_.focused.isPending)

  /** Происходит ли сейчас загрузка какой-либо карточки или карточек? */
  def adsHasPending: Boolean =
    ads.isPending || _adsHasPending

}
