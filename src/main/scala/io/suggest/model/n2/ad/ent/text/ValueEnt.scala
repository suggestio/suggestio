package io.suggest.model.n2.ad.ent.text

import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.ad.ent.Coords2d
import io.suggest.util.SioEsUtil.{DocField, FieldObject}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 16:21
 * Description: Абстрактная модель для entity-значений.
 *
 * Скорее всего её можно будет замёржить внутрь [[TextEnt]]-модель,
 * т.к. необходимость в существовании этой абстрактной value-модели отпала вместе с отказом
 * от float-полей (Price, Discount).
 */

object ValueEnt extends IGenEsMappingProps {

  val FONT_ESFN         = "font"
  val COORDS_ESFN       = "coords"
  val VALUE_ESFN        = "value"

  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(FONT_ESFN, enabled = false, properties = Nil),
      FieldObject(COORDS_ESFN, enabled = false, properties = Nil)
    )
  }

}


trait ValueEnt {

  def font: EntFont
  def coords: Option[Coords2d]

}
