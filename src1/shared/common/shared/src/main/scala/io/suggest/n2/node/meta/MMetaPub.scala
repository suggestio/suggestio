package io.suggest.n2.node.meta

import boopickle.Default._
import io.suggest.adn.edit.NodeEditConstants
import io.suggest.color.MColors
import io.suggest.common.empty.EmptyProduct
import io.suggest.scalaz.StringValidationNel
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 18:17
  * Description: Укороченная и кроссплатформенная реализация модели n2 MMeta, содержащая только совсем
  * публичные метаданные по узлу.
  * Содержит только публичные поля и только с портабельными данными.
  */
object MMetaPub {

  implicit val mMetaPubPickler: Pickler[MMetaPub] = {
    implicit val addressP = MAddress.mAddresPickler
    implicit val businessP = MBusinessInfo.mBusinessInfoPickler
    generatePickler[MMetaPub]
  }

  implicit def mMetaPubFormat: OFormat[MMetaPub] = (
    (__ \ "n").format[String] and
    (__ \ "a").format[MAddress] and
    (__ \ "b").format[MBusinessInfo] and
    (__ \ "c").format[MColors]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MMetaPub] = UnivEq.derive


  def validateName(name: String): StringValidationNel[String] =
    NodeEditConstants.Name.validateNodeName(name)

  def validate(metaPub: MMetaPub): StringValidationNel[MMetaPub] = {
    (
      validateName( metaPub.name ) |@|
      MAddress.validate( metaPub.address ) |@|
      MBusinessInfo.validate( metaPub.business ) |@|
      MColors.validateOrAdnSome( metaPub.colors )
    )(apply _)
  }

  val name      = GenLens[MMetaPub](_.name)
  val address   = GenLens[MMetaPub](_.address)
  val business  = GenLens[MMetaPub](_.business)
  val colors    = GenLens[MMetaPub](_.colors)

}


/** Класс-контейнер публичных кросс-платформенных мета-данных узла.
  *
  * @param name Имя узла.
  * @param address Данные по адресам.
  * @param business Бизнес-информация.
  * @param colors Цвета узла.
  */
case class MMetaPub(
                     name          : String,
                     address       : MAddress        = MAddress.empty,
                     business      : MBusinessInfo   = MBusinessInfo.empty,
                     colors        : MColors         = MColors.empty
                   )
  extends EmptyProduct
