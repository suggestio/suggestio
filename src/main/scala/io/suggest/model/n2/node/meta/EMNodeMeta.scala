package io.suggest.model.n2.node.meta

import io.suggest.model.{GenEsMappingPropsDummy, PrefixedFn}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 19:44
 * Description: Аддон для MNode-модели.
 */

object EMNodeMeta extends PrefixedFn {

  /** Имя поля на стороне ES, куда скидываются все метаданные. */
  val META_ESFN         = "meta"

  override protected def _PARENT_FN = META_ESFN

  def META_FLOOR_ESFN             = _fullFn( MNodeMeta.FLOOR_ESFN )
  def META_WELCOME_AD_ID_ESFN     = _fullFn( MNodeMeta.WELCOME_AD_ID )
  def META_NAME_ESFN              = _fullFn( MNodeMeta.NAME_ESFN )
  def META_NAME_SHORT_ESFN        = _fullFn( MNodeMeta.NAME_SHORT_ESFN )
  def META_NAME_SHORT_NOTOK_ESFN  = _fullFn( MNodeMeta.NAME_SHORT_NOTOK_ESFN )

}


import EMNodeMeta._


trait EMNodeMetaStatic extends GenEsMappingPropsDummy {

  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    FieldObject(META_ESFN, enabled = true, properties = MNodeMeta.generateMappingProps) ::
    super.generateMappingProps
  }

}
