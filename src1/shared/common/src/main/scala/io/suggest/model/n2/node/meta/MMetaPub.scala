package io.suggest.model.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.EmptyProduct
import io.suggest.model.n2.node.meta.colors.MColors
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

  implicit def univEq: UnivEq[MMetaPub] = UnivEq.derive

}


case class MMetaPub(
                     name          : String,
                     address       : MAddress        = MAddress.empty,
                     business      : MBusinessInfo   = MBusinessInfo.empty,
                     colors        : MColors         = MColors.empty
                   )
  extends EmptyProduct
