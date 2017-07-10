package io.suggest.sc.root.m

import diode.FastEq
import io.suggest.sc.m.MScNodeInfo

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
      a.currNode eq b.currNode
    }
  }

}


case class MScRoot(
                    currNode: Option[MScNodeInfo] = None
                  )
{

}
