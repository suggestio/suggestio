package io.suggest.model.n2.node.meta

import io.suggest.es.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:51
 * Description: ES-утиль для модель [[MAddress]].
 */
object MAddressEs extends IGenEsMappingProps {

  object Fields {
    val TOWN_FN         = "t"
    val ADDRESS_FN      = "a"
    val PHONE_FN        = "p"
    val FLOOR_FN        = "f"
    val SECTION_FN      = "s"
  }


  import Fields._

  implicit val FORMAT: OFormat[MAddress] = (
    (__ \ TOWN_FN).formatNullable[String] and
    (__ \ ADDRESS_FN).formatNullable[String] and
    (__ \ PHONE_FN).formatNullable[String] and
    (__ \ FLOOR_FN).formatNullable[String] and
    (__ \ SECTION_FN).formatNullable[String]
  )(MAddress.apply, unlift(MAddress.unapply))


  import io.suggest.es.util.SioEsUtil._
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(TOWN_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(ADDRESS_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(PHONE_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(FLOOR_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(SECTION_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true)
    )
  }

}