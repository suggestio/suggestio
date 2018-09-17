package io.suggest.pay

import io.suggest.bill.{Amount_t, MCurrency}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.04.17 12:05
  * Description: Модель платёжной информации о валюте для платежной системы.
  * Используется, чтобы описать поддержку той или иной валюты в рамках одной конкретной ПС.
  */

trait ICurrencyPayInfo {

  /** Валюта. */
  def currency: MCurrency


  /** Минимальный размер платежа, если известен. */
  def lowerDebtLimitOpt: Option[Amount_t]

  /** Минимальный размер платежа в текущей валюте или 0. */
  def lowerDebtLimit = lowerDebtLimitOpt.getOrElse(0d)


  /** Максимальный размер платежа, если есть. */
  def upperDebtLimitOpt: Option[Amount_t]

}


/** Дефолтовая реализация модели платежной инфы [[ICurrencyPayInfo]]. */
case class MCurrencyPayInfo(
                             override val currency            : MCurrency,
                             override val lowerDebtLimitOpt   : Option[Amount_t],
                             override val upperDebtLimitOpt   : Option[Amount_t]
                           )
  extends ICurrencyPayInfo

