package io.suggest.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.ValidationNel
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:10
  * Description: Модель метаданных городского адреса узла.
  */

object MAddress extends IEmpty with IEsMappingProps {

  object Fields {
    val TOWN_FN         = "t"
    val ADDRESS_FN      = "a"
  }


  import Fields._

  implicit val MADDRESS_FORMAT: OFormat[MAddress] = (
    (__ \ TOWN_FN).formatNullable[String] and
    (__ \ ADDRESS_FN).formatNullable[String]
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    Json.obj(
      TOWN_FN       -> FText.indexedJs,
      ADDRESS_FN    -> FText.indexedJs,
    )
  }

  override type T = MAddress

  override val empty: MAddress = {
    new MAddress() {
      override def nonEmpty = false
    }
  }

  implicit val mAddresPickler: Pickler[MAddress] = {
    generatePickler[MAddress]
  }

  @inline implicit def univEq: UnivEq[MAddress] = UnivEq.derive

  def validateTown(town: Option[String]): StringValidationNel[Option[String]] =
    ScalazUtil.validateTextOpt( town, maxLen = 40, "town" )

  def validateAddress(address: Option[String]): StringValidationNel[Option[String]] =
    ScalazUtil.validateTextOpt( address, maxLen = 100, "addr" )

  def validate(maddress: MAddress): ValidationNel[String, MAddress] = {
    (
      validateTown( maddress.town ) |@|
      validateAddress( maddress.address )
    )(apply _)
  }

  def town = GenLens[MAddress](_.town)
  def address = GenLens[MAddress](_.address)

}


case class MAddress(
                     town          : Option[String] = None,
                     address       : Option[String] = None,
                   )
  extends EmptyProduct
{

  def withTown(town: Option[String])          = copy(town = town)
  def withAddress(address: Option[String])    = copy(address = address)

}
