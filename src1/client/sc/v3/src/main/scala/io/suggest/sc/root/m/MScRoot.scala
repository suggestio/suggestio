package io.suggest.sc.root.m

import diode.FastEq
import diode.data.Pot
import io.suggest.geo.{MGeoLoc, MLocEnv}
import io.suggest.media.IMediaInfo
import io.suggest.routes.scRoutes
import io.suggest.sc.grid.m.MGridS
import io.suggest.sc.index.MWelcomeInfo
import io.suggest.sc.inx.m.MScIndex
import io.suggest.sc.styl.MScCssArgs
import io.suggest.ueq.UnivEqUtil._

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
      (a.dev      ===* b.dev) &&
        (a.index  ===* b.index) &&
        (a.grid   ===* b.grid) &&
        (a.jsRouter eq b.jsRouter)
    }
  }

}


case class MScRoot(
                    dev           : MScDev,
                    index         : MScIndex,
                    grid          : MGridS,
                    jsRouter      : Pot[scRoutes.type]      = Pot.empty
                  ) {

  def withDev( dev: MScDev )                        = copy(dev = dev)
  def withIndex( index: MScIndex )                  = copy(index = index)
  def withJsRouter( jsRouter: Pot[scRoutes.type] )  = copy(jsRouter = jsRouter)
  def withGrid( grid: MGridS )                      = copy(grid = grid)

  def locEnv: MLocEnv = {
    // TODO собрать данные по маячкам и текущей локации
    MLocEnv(
      geoLocOpt = for (gp <- index.state.geoPoint) yield {
        MGeoLoc(
          point = gp
        )
      }
    )
  }


  /** Инстанс модели аргументов рендера ScCss. */
  lazy val scCssArgs: MScCssArgs = {
    val indexRespOpt = index.resp.toOption
    val wcOpt = indexRespOpt.flatMap(_.welcome)
    def __whOpt(f: MWelcomeInfo => Option[IMediaInfo]) = wcOpt.flatMap(f).flatMap(_.whPx)

    MScCssArgs(
      customColorsOpt = indexRespOpt.map(_.colors),
      screen          = dev.screen.screen,
      wcBgWh          = __whOpt( _.bgImage ),
      wcFgWh          = __whOpt( _.fgImage )
    )
  }

}

