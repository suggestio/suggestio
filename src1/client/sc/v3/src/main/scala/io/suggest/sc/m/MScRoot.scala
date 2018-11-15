package io.suggest.sc.m

import diode.FastEq
import io.suggest.geo.{MGeoLoc, MLocEnv}
import io.suggest.sc.m.dev.MScDev
import io.suggest.sc.m.grid.MGridS
import io.suggest.sc.m.inx.MScIndex
import io.suggest.sc.sc3.MSc3Init
import io.suggest.sc.styl.MScCssArgs
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

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
      (a.dev          ===* b.dev) &&
        (a.index      ===* b.index) &&
        (a.grid       ===* b.grid) &&
        (a.internals  ===* b.internals)
    }
  }

  @inline implicit def univEq: UnivEq[MScRoot] = UnivEq.derive


  def scCssArgsFrom(scRoot: MScRoot): MScCssArgs = {
    MScCssArgs.from(scRoot.index.resp, scRoot.dev.screen.info)
  }

}


case class MScRoot(
                    dev           : MScDev,
                    index         : MScIndex,
                    grid          : MGridS,
                    internals     : MScInternals
                  ) {

  def withDev( dev: MScDev )                        = copy(dev = dev)
  def withIndex( index: MScIndex )                  = copy(index = index)
  def withInternals( internals: MScInternals )      = copy(internals = internals)
  def withGrid( grid: MGridS )                      = copy(grid = grid)

  def locEnvGeoLocOpt: Option[MGeoLoc] = {
    Some(
      MGeoLoc(
        point = index.search.geo.mapInit.state.center
      )
    )
  }
  def locEnvBleBeacons = dev.beaconer.nearbyReport
  def locEnv: MLocEnv = {
    MLocEnv(
      geoLocOpt  = locEnvGeoLocOpt,
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

