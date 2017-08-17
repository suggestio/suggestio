package io.suggest.ad.blk.ent

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.17 16:34
  * Description: JVM-only утиль для кроссовой модели MEntity.
  */
object MEntityJvm extends IGenEsMappingProps {

  override def generateMappingProps: List[DocField] = {
    val F = MEntity.Fields
    List(
      FieldNumber( F.ID_FN, index = false, include_in_all = false, fieldType = DocFieldTypes.integer),
      FieldObject( F.Text.TEXT_FN, enabled = true, properties = TextEntJvm.generateMappingProps),
      FieldObject( F.COORDS_ESFN, enabled = false, properties = Nil)
    )
  }

}
