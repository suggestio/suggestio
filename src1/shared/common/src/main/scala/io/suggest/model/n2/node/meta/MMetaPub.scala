package io.suggest.model.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.{EmptyProduct, IEmpty}

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

  implicit val mNodeadvMetaPickler: Pickler[MMetaPub] = {
    implicit val addressP = MAddress.mAddresPickler
    implicit val businessP = MBusinessInfo.mBusinessInfoPickler
    generatePickler[MMetaPub]
  }

}


case class MMetaPub(
                         address       : MAddress        = MAddress.empty,
                         business      : MBusinessInfo   = MBusinessInfo.empty
                       )
  extends EmptyProduct
