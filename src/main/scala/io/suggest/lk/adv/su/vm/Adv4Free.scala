package io.suggest.lk.adv.su.vm

import io.suggest.adv.AdvConstants.Su
import io.suggest.lk.adv.direct.m.Adv4FreeChanged
import io.suggest.lk.dt.interval.vm.InitLayoutFsmChange
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.input.Checked
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.16 15:59
 * Description: VM'ка для взаимодействия с чек-боксом суперюзеров.
 */
object Adv4Free extends FindElT {
  override type T       = Adv4Free
  override type Dom_t   = HTMLInputElement
  override def DOM_ID   = Su.ADV_FOR_FREE_ID
}


import Adv4Free.Dom_t


trait Adv4FreeT extends Checked with InitLayoutFsmChange {

  override type T = Dom_t
  override protected def _changeSignalModel = Adv4FreeChanged

}


case class Adv4Free(override val _underlying: Dom_t)
  extends Adv4FreeT
