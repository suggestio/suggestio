package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.lk.adv.m.MPriceS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 21:16
  * Description: Клиентская модель для свалки всяких маленьких значений из [[MRoot]],
  * [пока] не подходящих в другие или новые под-модели.
  *
  * Модель изначально не предназначена для сериализации, и может содержать любые значения.
  */
object MOther {

  implicit object MOtherFastEq extends FastEq[MOther] {
    override def eqv(a: MOther, b: MOther): Boolean = {
      (a.adId eq b.adId) &&
        (a.onMainScreen == b.onMainScreen) &&
        (a.price eq b.price)
    }
  }

}


/**
  * Класс модели "других" полей модели [[MRoot]].
  *
  * @param adId         id текущей рекламной карточки (размещаемого узла).
  *                     Приходит с сервера в рамках [[io.suggest.adv.geo.MFormInit]].
  *                     Передаётся на сервер внутри URL.
  *
  * @param onMainScreen Текущее Состояние галочки размещения на главном экране.
  * @param price Состояние ценника.
  */
case class MOther(
  adId          : String,
  onMainScreen  : Boolean             = true,
  price         : MPriceS             = MPriceS()
) {

  def withOnMainScreen(oms2: Boolean) = copy(onMainScreen = oms2)
  def withPriceS(price2: MPriceS) = copy(price = price2)

}
