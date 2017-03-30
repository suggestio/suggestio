package io.suggest.model.n2.node.meta

import boopickle.Default._
import io.suggest.common.empty.{EmptyProduct, IEmpty}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:10
  * Description: Модель метаданных городского адреса узла.
  */

object MAddress extends IEmpty {

  override type T = MAddress

  override val empty: MAddress = {
    new MAddress() {
      override def nonEmpty = false
    }
  }

  implicit val mAddresPickler: Pickler[MAddress] = {
    generatePickler[MAddress]
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
