package io.suggest.model.n2.node.common

import io.suggest.model.{FieldNamesL1, GenEsMappingPropsDummy}
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 15:37
 * Description:
 */
object EMNodeCommon {

  def COMMON_FN = FieldNamesL1.Common.name

}


import EMNodeCommon._


trait EMNodeCommonStatic extends GenEsMappingPropsDummy {

  override def generateMappingProps: List[DocField] = {
    FieldObject(COMMON_FN, enabled = true, properties = MNodeCommon.generateMappingProps) ::
      super.generateMappingProps
  }

}
