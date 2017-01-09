package io.suggest.mbill2.m.item.status

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 13:59
 * Description: Интерфейс к экземплярам моделей, имеющих поле item status.
 */
trait IMItemStatus {
  def status: MItemStatus
}
