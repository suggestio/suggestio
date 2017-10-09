package io.suggest.color

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil.{DocField, DocFieldTypes, FieldNumber}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 10:49
  * Description: ES-поддержка для кросс-платформенной модели MRgb.
  */
object MRgbEs extends IGenEsMappingProps {

  private def _field(fn: String) = FieldNumber(fn, fieldType = DocFieldTypes.integer, index = true, include_in_all = false)

  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = MRgb.Fields
    List(
      _field( F.RED_FN ),
      _field( F.GREEN_FN ),
      _field( F.BLUE_FN )
    )
  }

}
