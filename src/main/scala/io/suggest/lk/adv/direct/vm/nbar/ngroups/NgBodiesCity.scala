package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 19:19
 * Description: Контейнер всех [[NgBody]] в рамках города.
 */
object NgBodiesCity extends FindElDynIdT {

  override type DomIdArg_t  = String
  override type Dom_t       = HTMLDivElement
  override type T           = NgBodiesCity

  override def getDomId(arg: DomIdArg_t): String = {
    AdvDirectFormConstants.NGRPS_CITY_CONT_ID(arg)
  }

}


import NgBodiesCity.Dom_t


trait NgCityBodiesT extends CityIdT with ShowHideDisplayT {

  override type T = Dom_t

  def ngs: Iterator[NgBody] = {
    for (ngEl <- DomListIterator( _underlying.children )) yield {
      val ngEl1 = ngEl.asInstanceOf[ NgBody.Dom_t ]
      NgBody(ngEl1)
    }
  }

}


case class NgBodiesCity(override val _underlying: Dom_t)
  extends NgCityBodiesT
