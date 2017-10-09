package io.suggest.color

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil.{DocField, FieldKeyword, FieldNestedObject}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 11:01
  * Description: ES-поддержка для кросс-платформенной модели разных данных по цвету.
  *
  * Изначально, цвета не индексировались вообще, и данная ES-поддержка -- тоже не является обязательной.
  */
object MColorDataEs extends IGenEsMappingProps {


  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = MColorData.Fields
    List(
      FieldKeyword( F.CODE_FN, index = true, include_in_all = false ),
      FieldNestedObject( F.RGB_FN, enabled = true, properties = MRgbEs.generateMappingProps )
    )
  }

}
