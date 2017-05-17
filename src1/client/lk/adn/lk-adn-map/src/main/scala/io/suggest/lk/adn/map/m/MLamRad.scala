package io.suggest.lk.adn.map.m

import io.suggest.geo.MGeoCircle
import io.suggest.maps.m.{MRadS, MRadT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.17 13:55
  * Description: Состояние Rad-модели.
  */
object MLamRad {

  implicit def MLamRadFastEq = MRadT.MRadTFastEq

}


case class MLamRad(
                    override val circle      : MGeoCircle,
                    override val state       : MRadS
                  )
  extends MRadT[MLamRad]
{

  override def withCircle(circle2: MGeoCircle) = copy(circle = circle2)
  override def withState(state2: MRadS) = copy(state = state2)

  override def withCircleState(circle2: MGeoCircle, state2: MRadS) = {
    copy(
      circle = circle2,
      state  = state2
    )
  }

}
