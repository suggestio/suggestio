package io.suggest.n2.node.meta

import boopickle.Default._
import io.suggest.adn.edit.NodeEditConstants
import io.suggest.color.MColors
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
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
object MMetaPub extends IEmpty {

  override type T = MMetaPub

  def empty = apply()

  implicit def mMetaPubPickler: Pickler[MMetaPub] = {
    implicit val addressP = MAddress.mAddresPickler
    implicit val businessP = MBusinessInfo.mBusinessInfoPickler
    generatePickler[MMetaPub]
  }

  implicit def mMetaPubFormat: OFormat[MMetaPub] = (
    (__ \ "n").formatNullable[String] and
    (__ \ "a").formatNullable[MAddress]
      .inmap[MAddress](
        EmptyUtil.opt2ImplMEmptyF( MAddress ),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ "b").formatNullable[MBusinessInfo]
      .inmap[MBusinessInfo](
        EmptyUtil.opt2ImplMEmptyF( MBusinessInfo ),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ "c").formatNullable[MColors]
      .inmap[MColors](
        EmptyUtil.opt2ImplMEmptyF( MColors ),
        EmptyUtil.implEmpty2OptF
      )
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MMetaPub] = UnivEq.derive


  def validateName(name: String): StringValidationNel[String] = {
    NodeEditConstants.Name.validateNodeName(name)
  }

  def validate(metaPub: MMetaPub): StringValidationNel[MMetaPub] = {
    (
      ScalazUtil.liftNelOpt( metaPub.name.map(_.trim).filter(_.nonEmpty) )( validateName ) |@|
      MAddress.validate( metaPub.address ) |@|
      MBusinessInfo.validate( metaPub.business ) |@|
      MColors.validateOrAdnSome( metaPub.colors )
    )(apply _)
  }

  def name      = GenLens[MMetaPub](_.name)
  def address   = GenLens[MMetaPub](_.address)
  def business  = GenLens[MMetaPub](_.business)
  def colors    = GenLens[MMetaPub](_.colors)

}


/** Класс-контейнер публичных кросс-платформенных мета-данных узла.
  *
  * @param name Имя узла.
  * @param address Данные по адресам.
  * @param business Бизнес-информация.
  * @param colors Цвета узла.
  */
final case class MMetaPub(
                           name          : Option[String]  = None,
                           address       : MAddress        = MAddress.empty,
                           business      : MBusinessInfo   = MBusinessInfo.empty,
                           colors        : MColors         = MColors.empty
                         )
  extends EmptyProduct
