package io.suggest.model.n2.node.meta

import io.suggest.es.model.IGenEsMappingProps
import MBusinessInfo.Fields._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 15:04
 * Description: Модель метаданных по описанию делишек узла: инфа о товарах, посетителях, сайте и т.д.
 */
object MBusinessInfoEs extends IGenEsMappingProps {

  import io.suggest.es.util.SioEsUtil._
  override def generateMappingProps: List[DocField] = {
    List(
      FieldText(SITE_URL_FN, index = true, include_in_all = true, boost = Some(0.33F)),
      FieldText(AUDIENCE_DESCR_FN, index = true, include_in_all = false),
      FieldNumber(HUMAN_TRAFFIC_INT_FN, fieldType = DocFieldTypes.integer, index = true, include_in_all = false),
      FieldText(BDESCR_FN, index = true, include_in_all = true, boost = Some(0.1F))
    )
  }

}
