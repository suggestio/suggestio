package io.suggest.bill.cart.m

import diode.FastEq
import io.suggest.bill.cart.MCartConf
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 16:05
  * Description: Корневая модель состояния компонента полноценной корзины.
  */
object MCartRootS {

  implicit object MCartRootSFastEq extends FastEq[MCartRootS] {
    override def eqv(a: MCartRootS, b: MCartRootS): Boolean = {
      (a.conf ===* b.conf) &&
      (a.data ===* b.data)
    }
  }

  implicit def univEq: UnivEq[MCartRootS] = UnivEq.derive

}


/**
  * Корневая модель состояния корзины товаров и услуг.
  *
  * @param data Текущие данные заказа, полученные с сервера.
  * @param conf Данные конфигурации компонента, необходимые для запросов к серверу.
  */
case class MCartRootS(
                       conf       : MCartConf,
                       data       : MBillData,
                     ) {

  def withConf(conf: MCartConf) = copy(conf = conf)
  def withData(data: MBillData) = copy(data = data)

}
