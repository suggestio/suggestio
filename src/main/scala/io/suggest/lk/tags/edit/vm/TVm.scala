package io.suggest.lk.tags.edit.vm

import io.suggest.lk.tags.edit.vm.add.{AContainer, AddBtn}
import io.suggest.lk.tags.edit.vm.exist.EContainer
import io.suggest.lk.tags.edit.vm.search.hints.SContainer
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 14:19
 * Description: Корневая модель для tags edit верстки.
 */
object TVm extends IInitLayoutFsm {

  def existContainer = EContainer.find()

  def addContainer = AContainer.find()

  def addBtn = AddBtn.find()


  override def initLayout(fsm: SjsFsm): Unit = {
    val initF = IInitLayoutFsm.f(fsm)
    addContainer.foreach(initF)
    addBtn.foreach(initF)
    existContainer.foreach(initF)
  }

}
