package io.suggest.model.n2.node.meta

import io.suggest.common.EmptyProduct
import io.suggest.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:51
 * Description: Модель метаданных географического адреса узла.
 */
object MAddress extends IGenEsMappingProps {

  val TOWN_ESFN               = "t"
  val ADDRESS_ESFN            = "a"
  val PHONE_ESFN              = "p"
  val FLOOR_ESFN              = "f"
  val SECTION_ESFN            = "s"

  val empty: MAddress = {
    new MAddress() {
      override def nonEmpty = false
    }
  }

  implicit val FORMAT: OFormat[MAddress] = (
    (__ \ TOWN_ESFN).formatNullable[String] and
    (__ \ ADDRESS_ESFN).formatNullable[String] and
    (__ \ PHONE_ESFN).formatNullable[String] and
    (__ \ FLOOR_ESFN).formatNullable[String] and
    (__ \ SECTION_ESFN).formatNullable[String]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(TOWN_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(ADDRESS_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(PHONE_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(FLOOR_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(SECTION_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true)
    )
  }

}


case class MAddress(
  town          : Option[String] = None,
  address       : Option[String] = None,
  phone         : Option[String] = None,
  floor         : Option[String] = None,
  section       : Option[String] = None
)
  extends EmptyProduct
