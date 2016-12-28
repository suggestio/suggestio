package io.suggest.lk.adv.geo.m

import diode.data.Pot
import io.suggest.sjs.common.geo.json.GjFeature

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 21:50
  * Description: Корневая модель данных по каким-то текущим размещениям.
  */
case class MCurrGeoAdvs(
                         resp   : Pot[js.Array[GjFeature]]  = Pot.empty
                       ) {

  def withResp(resp2: Pot[js.Array[GjFeature]]) = copy(resp = resp2)

}
