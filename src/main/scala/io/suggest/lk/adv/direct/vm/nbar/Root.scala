package io.suggest.lk.adv.direct.vm.nbar

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.cities.{CityBodies, CitiesHeads}
import io.suggest.lk.adv.direct.vm.nbar.ngroups.NgCities
import io.suggest.sjs.common.fsm.{SjsFsm, IInitLayoutFsm}
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindDiv

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 18:54
 * Description: Корневой контейнер системы выбора узлов.
 */
object Root extends FindDiv {
  override type T     = Root
  override def DOM_ID = AdvDirectFormConstants.NODES_BAR_ID
}


import io.suggest.lk.adv.direct.vm.nbar.Root.Dom_t


trait RootT extends IVm with IInitLayoutFsm {

  override type T = Dom_t

  def cityHeads   = CitiesHeads.find()
  def cityBodies  = CityBodies.find()
  def ngCities    = NgCities.find()

  override def initLayout(fsm: SjsFsm): Unit = {
    val f = IInitLayoutFsm.f(fsm)
    cityHeads.foreach(f)
    cityBodies.foreach(f)
    ngCities.foreach(f)
  }

}


case class Root(override val _underlying: Dom_t)
  extends RootT
