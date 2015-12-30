package io.suggest.lk.adv.direct.vm.nbar.cities

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.sjs.common.vm.attr.AttrVmT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 15:15
 * Description: Добавить считывание id города из соотв. аттрибута текущего тега.
 */
trait CityIdT extends AttrVmT {

  /**
   * id города из аттрибута, если есть.
   * @return None если аттрибут отсутствует.
   *         Some(x) с es-id внутри.
   */
  def cityId = getAttribute( AdvDirectFormConstants.ATTR_CITY_ID )

}
