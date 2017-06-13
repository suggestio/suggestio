package io.suggest.common.empty

import diode.data.Pot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.06.17 12:16
  * Description: Поддержка diode Pot[_] для EmptyProduct.
  */
trait EmptyProductPot extends EmptyProduct { this: Product =>

  override protected[this] def _nonEmptyValue(v: Any): Boolean = {
    v match {
      case pot: Pot[_]  => pot.nonEmpty
      case other        => super._nonEmptyValue(other)
    }
  }

}
