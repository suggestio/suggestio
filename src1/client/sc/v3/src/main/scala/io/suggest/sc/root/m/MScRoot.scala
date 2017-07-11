package io.suggest.sc.root.m

import diode.FastEq
import diode.data.Pot
import io.suggest.sc.m.MScNodeInfo
import io.suggest.sc.router.routes

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
      (a.jsRouter eq b.jsRouter) &&
        (a.currNode eq b.currNode)
    }
  }

}


case class MScRoot(
                    jsRouter      : Pot[routes.type]      = Pot.empty,
                    currNode      : Option[MScNodeInfo]   = None
                  ) {

  def withJsRouter(jsRouter: Pot[routes.type]) = copy(jsRouter = jsRouter)

}

