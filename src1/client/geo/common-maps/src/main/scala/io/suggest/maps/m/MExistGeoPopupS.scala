package io.suggest.maps.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.geo.MGeoAdvExistPopupResp
import io.suggest.maps.OpenAdvGeoExistPopup
import japgolly.univeq.UnivEq
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.05.17 15:14
  * Description: Модель состояния попапа на карте над картой размещений, неявно-пустая.
  */

object MExistGeoPopupS {

  implicit object MGeoCurPopupSFastEq extends FastEq[MExistGeoPopupS] {
    override def eqv(a: MExistGeoPopupS, b: MExistGeoPopupS): Boolean = {
      (a.content ===* b.content) &&
        (a.state ===* b.state)
    }
  }

  @inline implicit def univEq: UnivEq[MExistGeoPopupS] = UnivEq.derive

}


case class MExistGeoPopupS(
                            content      : Pot[MGeoAdvExistPopupResp]    = Pot.empty,
                            state        : Option[OpenAdvGeoExistPopup]  = None
                          ) {

  def withContent( pot: Pot[MGeoAdvExistPopupResp] ) = copy( content = pot )
  def withState( state: Option[OpenAdvGeoExistPopup] ) = copy( state = state )

}
