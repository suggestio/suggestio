package io.suggest.model.n2.tag.vertex

import io.suggest.model.{EsModelT, FieldNamesL1, GenEsMappingPropsDummy}
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 16:33
 * Description: Аддоны для [[io.suggest.model.n2.MNode]] для духа одного глобального тега в системе.
 * В рамках архитектуры N2 эта модель живёт в качестве опционального свойства одного узла.
 */

object EMTagVertex {

  /** Имя поля тега. */
  def TAG_VERTEX_FN = FieldNamesL1.TagVertex.name

  /** JSON-десериализатор для опционального значения tag-поля. */
  implicit val READS: Reads[Option[MTagVertex]] = {
    (__ \ TAG_VERTEX_FN).readNullable[MTagVertex]
  }

  /** JSON-сериализатор для опционального значения tag-поля. */
  implicit val WRITES: OWrites[Option[MTagVertex]] = {
    (__ \ EMTagVertex.TAG_VERTEX_FN).writeNullable[MTagVertex]
  }

}


import io.suggest.model.n2.tag.vertex.EMTagVertex.TAG_VERTEX_FN


/** Аддон статической стороны [[io.suggest.model.n2.MNode]]. */
trait EMTagVertexStaticT extends GenEsMappingPropsDummy {

  override def generateMappingProps: List[DocField] = {
    FieldObject(TAG_VERTEX_FN, enabled = true, properties = MTagVertex.generateMappingProps) ::
      super.generateMappingProps
  }

}
