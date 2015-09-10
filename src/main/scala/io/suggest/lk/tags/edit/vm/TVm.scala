package io.suggest.lk.tags.edit.vm

import io.suggest.lk.tags.edit.vm.add.AContainer
import io.suggest.lk.tags.edit.vm.exist.EContainer
import io.suggest.sjs.common.vm.util.IInitLayout

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 14:19
 * Description: Корневая модель для tags edit верстки.
 */
object TVm extends IInitLayout {

  def existContainer = EContainer.find()

  def addContainer = AContainer.find()

  override def initLayout(): Unit = {
    val initF = IInitLayout.f
    addContainer foreach initF
    existContainer foreach initF
  }

}
