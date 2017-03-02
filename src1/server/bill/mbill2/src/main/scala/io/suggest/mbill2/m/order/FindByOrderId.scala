package io.suggest.mbill2.m.order

import io.suggest.mbill2.m.common.ModelContainer
import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 21:40
  * Description: Поддержка упрощенной сборки экшена выборки по колонке order_id.
  */
trait FindByOrderId extends ModelContainer { that: OrderIdSlick =>

  import profile.api._

  override type Table_t <: Table[El_t] with OrderId

  def findByOrderIdBuilder(orderId: Gid_t) = {
    query.filter(_.orderId === orderId)
  }

  def findByOrderId(orderId: Gid_t): DBIOAction[Seq[El_t], NoStream, Effect.Read] = {
    findByOrderIdBuilder(orderId).result
  }

}
