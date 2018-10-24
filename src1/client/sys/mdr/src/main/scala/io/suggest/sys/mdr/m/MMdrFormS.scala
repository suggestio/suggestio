package io.suggest.sys.mdr.m

import diode.FastEq
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.10.18 18:16
  * Description: Рантаймовый конфиг формы.
  */
object MMdrFormS {

  def empty = apply()

  implicit object MMdrFormSFastEq extends FastEq[MMdrFormS] {
    override def eqv(a: MMdrFormS, b: MMdrFormS): Boolean = {
      (a.forceAllRcrvs ==* b.forceAllRcrvs)
    }
  }

  implicit def univEq: UnivEq[MMdrFormS] = UnivEq.derive

}


/** Контейнер рантаймового конфига формы.
  *
  * @param forceAllRcrvs Форсировать поиск модерации за пределами текущего ресивера, если он задан.
  *                      На сервер не будет отсылаться id/key текущего ресивера в запросах.
  */
case class MMdrFormS(
                      forceAllRcrvs     : Boolean         = false
                    ) {

  def withForceAllRcvrs(forceAllRcrvs: Boolean) = copy(forceAllRcrvs = forceAllRcrvs)

}
