package io.suggest.mbill2.m.order

import io.suggest.mbill2.m.common.ModelContainer
import io.suggest.mbill2.m.gid.{GidSlick, Gid_t}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 21:40
  * Description: Поддержка упрощенной сборки экшена выборки по колонке order_id.
  */
trait FindByOrderId extends ModelContainer { that: OrderIdSlick with GidSlick =>

  import profile.api._

  override type Table_t <: Table[El_t] with OrderId with GidColumn

  def findByOrderIdBuilder(orderIds: Gid_t*) = _findByOrderIdBuilder(orderIds)

  private def _findByOrderIdBuilder(orderIds: Traversable[Gid_t]) = {
    query.filter(_.orderId inSet orderIds)
  }

  def findByOrderId(orderIds: Gid_t*): DBIOAction[Seq[El_t], Streaming[El_t], Effect.Read] = {
    _findByOrderIdBuilder(orderIds)
      .result
  }

  def findIdsByOrderId(orderIds: Gid_t*): DBIOAction[Seq[Gid_t], Streaming[Gid_t], Effect.Read] = {
    _findByOrderIdBuilder(orderIds)
      .map(_.id)
      .result
  }

}
