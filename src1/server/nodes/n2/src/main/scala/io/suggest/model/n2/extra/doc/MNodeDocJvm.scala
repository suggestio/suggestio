package io.suggest.model.n2.extra.doc

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 16:10
  * Description: JVM-only поддержка для модели документа внутри узла.
  */
@deprecated
object MNodeDocJvm extends IGenEsMappingProps {

  /** Сборка схемы ES. */
  def generateMappingProps: List[DocField] = {
    val F = MNodeDoc.Fields
    List(
      // Структура doc рекурсивна, древовидна и не содержит индексируемых данных
      // (за искл. параметров рендера jd-тегов, которые можно с трудом как-то проиндексировать для сбора сомнительной статистики).
      FieldObject(
        id = F.TEMPLATE_FN,
        properties = Nil,
        enabled = false
      )
    )
  }

}
