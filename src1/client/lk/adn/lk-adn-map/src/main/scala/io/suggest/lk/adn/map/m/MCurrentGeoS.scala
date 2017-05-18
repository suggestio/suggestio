package io.suggest.lk.adn.map.m

import diode.FastEq
import diode.data.Pot
import io.suggest.sjs.common.geo.json.GjFeature

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.17 15:23
  * Description: Модель-контейнер полей, описывающих состояния по текущим георазмещениям узла
  * и попапам.
  */
object MCurrentGeoS {

  /** Поддержка FastEq для инстансов [[MCurrentGeoS]]. */
  implicit object MCurrentGeoSFastEq extends FastEq[MCurrentGeoS] {
    override def eqv(a: MCurrentGeoS, b: MCurrentGeoS): Boolean = {
      a.existingGj eq b.existingGj
    }
  }

}


case class MCurrentGeoS(
                         existingGj   : Pot[js.Array[GjFeature]]    = Pot.empty
                       ) {

  def withExistingGj( pot: Pot[js.Array[GjFeature]] ) = copy( existingGj = pot )

}
