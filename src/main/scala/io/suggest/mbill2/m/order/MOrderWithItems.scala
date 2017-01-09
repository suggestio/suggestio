package io.suggest.mbill2.m.order

import io.suggest.mbill2.m.item.MItem

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 13:17
  * Description: Виртуальная модель-контейнер для ордера с item'ами.
  */
trait IOrderWithItems {

  def morder: MOrder

  def mitems: Seq[MItem]

}


/** Дефолтовая реализация модели [[IOrderWithItems]]. */
case class MOrderWithItems(
  override val morder: MOrder,
  override val mitems: Seq[MItem]
)
  extends IOrderWithItems
