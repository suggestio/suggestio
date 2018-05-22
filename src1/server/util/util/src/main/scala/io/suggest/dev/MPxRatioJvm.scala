package io.suggest.dev

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.playx.FormMappingUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.18 17:31
  * Description: JVM-костыли для модели DevPixelRatio.
  */

object MPxRatioJvm {

  def mappingOpt = EnumeratumJvmUtil.shortIdOptMapping( MPxRatios )
  def mapping = FormMappingUtil.optMapping2required( mappingOpt )

}

