package io.suggest.sc.m

import diode.FastEq
import io.suggest.ble.MUidBeacon
import io.suggest.geo.{MGeoLoc, MLocEnv}
import io.suggest.sc.m.dev.MScDev
import io.suggest.sc.m.dia.MScDialogs
import io.suggest.sc.m.grid.MGridS
import io.suggest.sc.m.in.MScInternals
import io.suggest.sc.m.inx.MScIndex
import io.suggest.sc.m.styl.MScCssArgs
import io.suggest.sc.sc3.MSc3Init
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 12:23
  * Description: Корневая модель состояния выдачи v3.
  * Всё, что описывает основной интерфейс выдачи, должно быть описано тут.
  */
object MScRoot {

  implicit case object MScRootFastEq extends FastEq[MScRoot] {
    override def eqv(a: MScRoot, b: MScRoot): Boolean = {
      (a.dev        ===* b.dev) &&
      (a.index      ===* b.index) &&
      (a.grid       ===* b.grid) &&
      (a.internals  ===* b.internals) &&
      (a.dialogs    ===* b.dialogs)
    }
  }

  @inline implicit def univEq: UnivEq[MScRoot] = UnivEq.derive


  def scCssArgsFrom(scRoot: MScRoot): MScCssArgs = {
    MScCssArgs.from(scRoot.index.resp, scRoot.dev.screen.info)
  }

  /** "Линзы" для упрощённого доступа к полям нижнего уровня. */
  val dev       = GenLens[MScRoot](_.dev)
  val index     = GenLens[MScRoot](_.index)
  val grid      = GenLens[MScRoot](_.grid)
  val internals = GenLens[MScRoot](_.internals)
  val dialogs   = GenLens[MScRoot](_.dialogs)

}


/** Корневой контейнер данных состояния выдачи.
  *
  * @param dev Оборудование.
  * @param index Интерфейс.
  * @param grid Плитка.
  * @param internals Внутренности.
  * @param dialogs Диалоги.
  */
case class MScRoot(
                    dev           : MScDev,
                    index         : MScIndex,
                    grid          : MGridS,
                    internals     : MScInternals,
                    dialogs       : MScDialogs    = MScDialogs.empty,
                  ) {

  def userLocOpt: Option[MGeoLoc] = {
    index.search.geo.mapInit.userLoc
  }
  def geoLocOpt: Option[MGeoLoc] = {
    Some(
      MGeoLoc(
        point = index.search.geo.mapInit.state.center
      )
    )
  }

  def locEnvBleBeacons = {
    val nr0 = dev.beaconer.nearbyReport
    internals.info.currRoute
      .filter(_.virtBeacons.nonEmpty)
      .fold( nr0 ) { mainScreen =>
        mainScreen
          .virtBeacons
          .iterator
          .map(MUidBeacon(_, 0)) ++: nr0
      }
  }
  def locEnvMap: MLocEnv = {
    MLocEnv(
      geoLocOpt  = geoLocOpt,
      bleBeacons = locEnvBleBeacons
    )
  }
  def locEnvUser: MLocEnv = {
    MLocEnv(
      geoLocOpt  = userLocOpt,
      bleBeacons = locEnvBleBeacons
    )
  }

  /** Перегонка в инстанс MSc3Init. */
  def toScInit: MSc3Init = {
    MSc3Init(
      mapProps  = index.search.geo.mapInit.state.toMapProps,
      conf      = internals.conf,
      // TODO clientTimstamp
    )
  }

}

