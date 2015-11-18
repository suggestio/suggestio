package io.suggest.model.n2.tag.vertex

import io.suggest.model.n2.FieldNamesL1
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 16:33
 * Description: Аддоны для [[io.suggest.model.n2.node.MNode]] для духа одного глобального тега в системе.
 * В рамках архитектуры N2 эта модель живёт в качестве опционального свойства одного узла.
 */

object EMTagVertex {

  /** Имя поля тега. */
  def TAG_VERTEX_FN = FieldNamesL1.TagVertex.name

  /** JSON-сериализатор и десериализатор для опционального значения tag-поля. */
  val FORMAT: OFormat[Option[MTagVertex]] = {
    (__ \ TAG_VERTEX_FN).formatNullable[MTagVertex]
  }

}
