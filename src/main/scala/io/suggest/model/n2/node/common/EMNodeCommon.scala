package io.suggest.model.n2.node.common

import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.{FieldNamesL1, GenEsMappingPropsDummy}
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 15:37
 * Description:
 */
object EMNodeCommon {

  def COMMON_FN = FieldNamesL1.Common.name

  val FORMAT: OFormat[MNodeCommon] = {
    (__ \ COMMON_FN)
      .formatNullable(MNodeCommon.FORMAT)
      .inmap[MNodeCommon](
        { _ getOrElse MNodeCommon(MNodeTypes.Tag, isDependent = true) },
        { Some.apply }
      )
  }

  def READS: Reads[MNodeCommon] = FORMAT
  def WRITES: OWrites[MNodeCommon] = FORMAT

}


import EMNodeCommon._


trait EMNodeCommonStatic extends GenEsMappingPropsDummy {

  override def generateMappingProps: List[DocField] = {
    FieldObject(COMMON_FN, enabled = true, properties = MNodeCommon.generateMappingProps) ::
      super.generateMappingProps
  }

}
