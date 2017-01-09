package io.suggest.sc.sjs.vm.maps

import io.suggest.common.maps.mapbox.MapBoxConstants
import io.suggest.sjs.common.vm.attr.StringInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.rm.SelfRemoveT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 19:39
  * Description: VM'ка для доступа к input'у MapBox GL Access Token.
  */
object MpglAcTok extends FindElT {
  override def DOM_ID   = MapBoxConstants.ACCESS_TOKEN_INPUT_ID
  override type Dom_t   = HTMLInputElement
  override type T       = MpglAcTok
}


import MpglAcTok.Dom_t

trait MpglAcTokT extends StringInputValueT with SelfRemoveT {
  override type T = Dom_t
}


case class MpglAcTok(override val _underlying: Dom_t)
  extends MpglAcTokT
