package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.sjs.common.vm.spa.IOverMPots
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 16:14
  * Description: Модель состояния попапов.
  */
object MPopupsS {

  implicit object MPopupsFastEq extends FastEq[MPopupsS] {
    override def eqv(a: MPopupsS, b: MPopupsS): Boolean = {
      a.nodeInfo ===* b.nodeInfo
    }
  }

  @inline implicit def univEq: UnivEq[MPopupsS] = UnivEq.derive

}


/** Модель опциональных состояний разных попапов формы.
  *
  * @param nodeInfo Опциональное состояние попапа с инфой по узлу.
  */
case class MPopupsS(
                     nodeInfo   : Option[MNodeInfoPopupS]   = None
                   )
  extends EmptyProduct
  with IOverMPots
{

  def withNodeInfo(nodeInfo2: Option[MNodeInfoPopupS]) = copy(nodeInfo = nodeInfo2)

}
