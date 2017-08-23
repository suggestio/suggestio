package io.suggest.model.n2.edge

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import io.suggest.util.SioConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 14:13
  * Description: Jvm-only утиль для кросс-платформенной модели MEdgeDoc.
  */
object MEdgeDocJvm extends IGenEsMappingProps {

  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    val F = MEdgeDoc.Fields
    List(
      FieldNumber(
        id = F.UID_FN,
        fieldType = DocFieldTypes.integer,
        index = false,
        include_in_all = false
      ),

      // Текст следует индексировать по-нормальному. Потом в будущем схема индексации неизбежно будет расширена.
      FieldText(
        id = F.TEXT_FN,
        index = true,
        include_in_all = true,
        // Скопипасчено с MNode._all. Начиная с ES-6.0, поле _all покидает нас, поэтому тут свой индекс.
        analyzer = SioConstants.ENGRAM_AN_1,
        search_analyzer = SioConstants.DFLT_AN
      )
    )
  }

}
