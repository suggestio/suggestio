package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.lk.adv.direct.m.CityNgIdOpt
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.lk.adv.direct.vm.nbar.ngroups.NgIdT
import io.suggest.sjs.common.vm.find.FindElDynIdT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.01.16 18:41
  * Description: Поддержка быстрого доступа к опциональному CityNgIdOpt.
  */
trait WithCityNgIdOpt extends CityIdT with NgIdT {

  /**
   * Собрать экземпляр CityNgIdOpt и использовать его для поиска в другой VM'ке.
   * @param vm Статическая VM.
   * @tparam X vm.T.
   * @return Опциональный экземпляр динамической VM.
   */
  def _findByCityNgIdOpt[X](vm: FindElDynIdT { type DomIdArg_t = CityNgIdOpt; type T = X }): Option[X] = {
    cityId.flatMap { _cityId =>
      val arg = CityNgIdOpt(cityId.get, ngId)
      vm.find(arg)
    }
  }

}
