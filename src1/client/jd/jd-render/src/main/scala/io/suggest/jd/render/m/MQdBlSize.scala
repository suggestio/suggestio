package io.suggest.jd.render.m

import io.suggest.common.geom.d2.MSize2di
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.10.2019 15:52
  * Description: Модель данных по размерам qd-blockless-блока.
  */
object MQdBlSize {

  @inline implicit def univEq: UnivEq[MQdBlSize] = UnivEq.derive

}

case class MQdBlSize(
                      bounds: MSize2di,
                      client: MSize2di,
                    )
