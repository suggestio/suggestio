package io.suggest.model.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.{EmptyProduct, IEmpty}
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
object MMetaPub extends IEmpty {

  override type T = MMetaPub

  def empty = MMetaPub()

  implicit val mMetaPubPickler: Pickler[MMetaPub] = {
    implicit val addressP = MAddress.mAddresPickler
    implicit val businessP = MBusinessInfo.mBusinessInfoPickler
    generatePickler[MMetaPub]
  }

  implicit def mMetaPubFormat: OFormat[MMetaPub] = (
    (__ \ "a").format[MAddress] and
    (__ \ "b").format[MBusinessInfo]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MMetaPub] = UnivEq.derive

}


case class MMetaPub(
                         address       : MAddress        = MAddress.empty,
                         business      : MBusinessInfo   = MBusinessInfo.empty
                       )
  extends EmptyProduct
