package io.suggest.lk.adn.map.vm

import io.suggest.adn.mapf.AdnMapFormConstants
import io.suggest.lk.adv.vm.Adv4FreeInside
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.input.FormDataVmT
import io.suggest.sjs.dt.period.vm.IContainerField
import org.scalajs.dom.raw.HTMLFormElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 18:16
  * Description: vm'ка для всей формы размещения узла на карте.
  */
object LamForm extends FindElT {
  override type Dom_t = HTMLFormElement
  override type T     = LamForm
  override def DOM_ID = AdnMapFormConstants.FORM_ID
}

import LamForm.Dom_t

case class LamForm(override val _underlying: Dom_t)
  extends FormDataVmT
  with Adv4FreeInside
  with IContainerField
{

  override type T = Dom_t

  def tzOffField = InpTzOffset.find()

  override def initLayout(fsm: SjsFsm): Unit = {
    super.initLayout(fsm)
    val f = IInitLayoutFsm.f(fsm)
    intervalCont.foreach(f)
    tzOffField.foreach(f)
  }

}