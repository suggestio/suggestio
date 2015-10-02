package io.suggest.model.n2.edge

import io.suggest.model.{FieldNamesL1, GenEsMappingPropsDummy}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 11:41
 * Description: Поддержка эджей для модели MNode.
 */
object EMNodeEdges {

  def EDGES_FN = FieldNamesL1.Edges.name

  val FORMAT: OFormat[MNodeEdges] = {
    (__ \ EDGES_FN).formatNullable[MNodeEdges]
      .inmap [MNodeEdges] (
        _ getOrElse MNodeEdges.empty,
        { mne => if (mne.nonEmpty) Some(mne) else None }
      )
  }

}


import EMNodeEdges.EDGES_FN


trait EMNodeEdgesStatic extends GenEsMappingPropsDummy {

  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    FieldObject(EDGES_FN, enabled = true, properties = MNodeEdges.generateMappingProps) ::
    super.generateMappingProps
  }

}
