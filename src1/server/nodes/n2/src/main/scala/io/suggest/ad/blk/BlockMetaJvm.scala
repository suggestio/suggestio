package io.suggest.ad.blk

import io.suggest.es.model.IGenEsMappingProps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.17 14:15
  * Description: Доп.поддержка кросс-платформенной модели BlockMeta на стороне JVM.
  */
object BlockMetaJvm extends IGenEsMappingProps {

  import io.suggest.es.util.SioEsUtil._

  private def _fint(fn: String) = {
    FieldNumber(
      id              = fn,
      fieldType       = DocFieldTypes.integer,
      index           = true,
      include_in_all  = false
    )
  }

  def generateMappingProps: List[DocField] = {
    val F = BlockMeta.Fields
    List(
      _fint( F.HEIGHT),
      _fint( F.WIDTH),
      FieldKeyword( F.EXPAND_MODE, index = true, include_in_all = false ),
    )
  }

}
