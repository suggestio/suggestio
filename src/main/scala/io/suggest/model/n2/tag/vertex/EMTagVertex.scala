package io.suggest.model.n2.tag.vertex

import io.suggest.model.n2.{FieldNamesL1, node}
import io.suggest.model.n2.node.MNode
import io.suggest.model.GenEsMappingPropsDummy
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 16:33
 * Description: Аддоны для [[node.MNode]] для духа одного глобального тега в системе.
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


import io.suggest.model.n2.tag.vertex.EMTagVertex.TAG_VERTEX_FN


/** Аддон статической стороны [[MNode]]. */
trait EMTagVertexStaticT extends GenEsMappingPropsDummy {

  override def generateMappingProps: List[DocField] = {
    FieldObject(TAG_VERTEX_FN, enabled = true, properties = MTagVertex.generateMappingProps) ::
      super.generateMappingProps
  }

}
