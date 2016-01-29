package io.suggest.lk.adv.direct.vm

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.Root
import io.suggest.lk.adv.su.vm.Adv4Free
import io.suggest.lk.dt.interval.vm.Container
import io.suggest.sjs.common.fsm.{SjsFsm, IInitLayoutFsm}
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.input.FormDataVmT
import org.scalajs.dom.raw.HTMLFormElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 9:55
 * Description: VM'ка всея формы прямого размещения.
 */
object Form extends FindElT {
  override type T     = Form
  override type Dom_t = HTMLFormElement
  override def DOM_ID = AdvDirectFormConstants.FORM_ID
}


import Form.Dom_t


trait FormT
  extends FormDataVmT
  with IInitLayoutFsm
{

  override type T = Dom_t

  def root          = Root.find()
  def intervalCont  = Container.find()
  def adv4free      = Adv4Free.find()

  override def initLayout(fsm: SjsFsm): Unit = {
    val f = IInitLayoutFsm.f(fsm)
    root.foreach(f)
    intervalCont.foreach(f)
    adv4free.foreach(f)
  }

}


case class Form(override val _underlying: Dom_t)
  extends FormT
