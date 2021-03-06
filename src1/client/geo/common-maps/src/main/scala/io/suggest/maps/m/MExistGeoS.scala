package io.suggest.maps.m

import diode.FastEq
import diode.data.Pot
import io.suggest.geo.json.GjFeature
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 21:50
  * Description: Корневая модель данных по каким-то текущим размещениям.
  */
object MExistGeoS {

  /** Поддержка diode FastEq для инстансов [[MExistGeoS]]. */
  implicit object MExistGeoSFastEq extends FastEq[MExistGeoS] {
    override def eqv(a: MExistGeoS, b: MExistGeoS): Boolean = {
      (a.geoJson ===* b.geoJson) &&
        (a.popup ===* b.popup)
    }
  }

  @inline implicit def univEq: UnivEq[MExistGeoS] = UnivEq.derive

}


case class MExistGeoS(
                       geoJson   : Pot[js.Array[GjFeature]]     = Pot.empty,
                       popup     : MExistGeoPopupS              = MExistGeoPopupS()
                   ) {

  def withGeoJson(resp2: Pot[js.Array[GjFeature]])    = copy(geoJson = resp2)
  def withPopup(popup: MExistGeoPopupS)               = copy(popup = popup)

}
