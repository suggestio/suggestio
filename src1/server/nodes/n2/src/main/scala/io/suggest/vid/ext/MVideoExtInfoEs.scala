package io.suggest.vid.ext

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 21:02
  * Description: Доп.поддержка кросс-платформенной модели MVideoExtInfo на стороне сервера.
  */
@deprecated
object MVideoExtInfoEs extends IGenEsMappingProps {

  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = MVideoExtInfo.Fields
    List(
      FieldKeyword(F.VIDEO_SERVICE_FN, index = true, include_in_all = false),
      FieldKeyword(F.REMOTE_ID_FN, index = true, include_in_all = true)
    )
  }

}
