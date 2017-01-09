package io.suggest.mbill2.m.price

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:40
 * Description: Интерфейс к экземплярам моделей к полю с ценой.
 */
trait IMPrice {

  /** Цена: значение + валюта. */
  def price: MPrice

}
