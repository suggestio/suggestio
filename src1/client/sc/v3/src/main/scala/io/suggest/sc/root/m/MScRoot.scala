package io.suggest.sc.root.m

import diode.FastEq
import diode.data.Pot
import io.suggest.geo.MLocEnv
import io.suggest.routes.scRoutes
import io.suggest.sc.inx.m.MScIndex

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
      (a.index eq b.index) &&
        (a.jsRouter eq b.jsRouter)
    }
  }

}


case class MScRoot(
                    index         : MScIndex,
                    jsRouter      : Pot[scRoutes.type]      = Pot.empty
                  ) {

  def withIndex( index: MScIndex ) = copy(index = index)
  def withJsRouter( jsRouter: Pot[scRoutes.type] ) = copy(jsRouter = jsRouter)

  def locEnv: MLocEnv = {
    // TODO собрать данные по маячкам и текущей локации
    MLocEnv.empty
  }

}

