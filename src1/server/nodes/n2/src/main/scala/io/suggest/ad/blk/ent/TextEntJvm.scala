package io.suggest.ad.blk.ent

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil.{DocField, FieldObject, FieldText}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.17 16:19
  * Description: JVM-поддержка для text entities.
  */

object TextEntJvm extends IGenEsMappingProps {

  override def generateMappingProps: List[DocField] = {
    val fstr = FieldText(
      id              = ValueEnt.VALUE_ESFN,
      index           = false,
      include_in_all  = true,
      boost           = Some(1.1F)
    )
    List(
      fstr,
      FieldObject(ValueEnt.FONT_ESFN, enabled = false, properties = Nil)
    )
  }

}
