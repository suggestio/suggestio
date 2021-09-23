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


  def scCssArgsFrom(scRoot: MScRoot): MScCssArgs =
    MScCssArgs.from( scRoot.index.respOpt, scRoot.dev.screen.info )

  /** "Линзы" для упрощённого доступа к полям нижнего уровня. */
  val dev       = GenLens[MScRoot](_.dev)
  val index     = GenLens[MScRoot](_.index)
  def grid      = GenLens[MScRoot](_.grid)
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

  def locEnvRadioBeacons = {
    val nearbyReport0 = dev.beaconer.nearbyReport
    internals.info.currRoute
      .filter(_.virtBeacons.nonEmpty)
      .fold( nearbyReport0 ) { mainScreen =>
        mainScreen
          .virtBeacons
          .iterator
          .map { MUidBeacon(_) } ++: nearbyReport0
      }
  }
  def locEnvMap: MLocEnv = {
    MLocEnv(
      geoLoc     = geoLocOpt.toList,
      beacons    = locEnvRadioBeacons,
    )
  }
  def locEnvUser: MLocEnv = {
    MLocEnv(
      // Testing several geolocations at once, because current "user-expected location" is too abstract to be sure about one concrete value.
      geoLoc     = (userLocOpt :: geoLocOpt :: Nil)
        .iterator
        .flatten
        .distinct
        .toList,
      beacons    = locEnvRadioBeacons,
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

