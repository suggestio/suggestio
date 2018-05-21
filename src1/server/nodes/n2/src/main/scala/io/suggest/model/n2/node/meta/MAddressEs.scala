package io.suggest.model.n2.node.meta

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import MAddress.Fields._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:51
 * Description: ES-утиль для модель [[MAddress]].
 */
object MAddressEs extends IGenEsMappingProps {

  override def generateMappingProps: List[DocField] = {
    List(
      FieldText(TOWN_FN, index = true, include_in_all = true),
      FieldText(ADDRESS_FN, index = true, include_in_all = true)
    )
  }

}
