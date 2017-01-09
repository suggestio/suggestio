package io.suggest.lk.adv.geo.tags.vm

import io.suggest.adv.geo.AdvGeoConstants.OnMainScreen
import io.suggest.lk.adv.geo.tags.m.signal.OnMainScreenChanged
import io.suggest.sjs.common.fsm.InitLayoutFsmChange
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.input.Checked
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 15:18
  * Description: vm'ка взаимодействия с галочкой размещения на главном экране.
  */

object OnMainScrCb extends FindElT {
  override type Dom_t = HTMLInputElement
  override type T     = OnMainScrCb
  override def DOM_ID = OnMainScreen.ID
}


import OnMainScrCb.Dom_t

trait OnMainScrCbT extends VmT with Checked with InitLayoutFsmChange {

  override type T = Dom_t
  override protected def _changeSignalModel = OnMainScreenChanged
}


case class OnMainScrCb(override val _underlying: Dom_t)
  extends OnMainScrCbT
