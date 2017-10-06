package io.suggest.common.geom.d2

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil.{DocField, DocFieldTypes, FieldNumber}
import io.suggest.media.MediaConst.NamesShort

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 15:32
  * Description: Поддержка ES для кросс-платформенной модели MSize2di.
  */
object MSize2diEs extends IGenEsMappingProps {

  // Имена полей короткие, заданы в MediaConst.NamesShort.

  private def _numberField(fn: String) = {
    FieldNumber(fn, fieldType = DocFieldTypes.integer, index = true, include_in_all = false)
  }

  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = NamesShort
    List(
      _numberField( F.WIDTH_FN ),
      _numberField( F.HEIGHT_FN )
    )
  }

  // Без JSON тут, потому что OFormat из [common] MSize2di достаточен и для ES.

}
