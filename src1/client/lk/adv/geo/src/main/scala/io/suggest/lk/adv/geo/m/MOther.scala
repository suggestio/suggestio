package io.suggest.lk.adv.geo.m

import diode.FastEq

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
        (a.doc eq b.doc)
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
  */
case class MOther(
  adId          : String,
  onMainScreen  : Boolean             = true,
  doc           : MDocS               = MDocS()
) {

  def withOnMainScreen(oms2: Boolean) = copy(onMainScreen = oms2)
  def withDoc(doc2: MDocS) = copy(doc = doc2)

}
