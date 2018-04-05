package io.suggest.model.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:10
  * Description: Модель метаданных городского адреса узла.
  */

object MAddress extends IEmpty {

  object Fields {
    val TOWN_FN         = "t"
    val ADDRESS_FN      = "a"
    val PHONE_FN        = "p"
    val FLOOR_FN        = "f"
    val SECTION_FN      = "s"
  }


  import Fields._

  implicit val MADDRESS_FORMAT: OFormat[MAddress] = (
    (__ \ TOWN_FN).formatNullable[String] and
    (__ \ ADDRESS_FN).formatNullable[String] and
    (__ \ PHONE_FN).formatNullable[String] and
    (__ \ FLOOR_FN).formatNullable[String] and
    (__ \ SECTION_FN).formatNullable[String]
  )(apply, unlift(unapply))


  override type T = MAddress

  override val empty: MAddress = {
    new MAddress() {
      override def nonEmpty = false
    }
  }

  implicit val mAddresPickler: Pickler[MAddress] = {
    generatePickler[MAddress]
  }

  implicit def univEq: UnivEq[MAddress] = UnivEq.derive

}


case class MAddress(
                     town          : Option[String] = None,
                     address       : Option[String] = None,
                     phone         : Option[String] = None,
                     floor         : Option[String] = None,
                     section       : Option[String] = None
                   )
  extends EmptyProduct
