package io.suggest.bill.tf.daily

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 17:05
  * Description: Констатны посуточных тарифов.
  */
object TfDailyConst {

  /** Константы ценников. */
  object Amount {

    /** Минимальный amount. */
    def MIN = 0.05

    /** Максимальное значение amount ДЛЯ БУДНЕЙ. На выходные и прайм-тайм это влияет только пропорционально. */
    def MAX = 1000.0

  }

}
