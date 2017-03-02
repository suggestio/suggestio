package io.suggest.mbill2.m.common

import io.suggest.slick.profile.IProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 22:19
 * Description: Типа-интерфейс для контейнеров slick-моделей.
 */
trait ModelContainer extends IProfile {

  import profile.api._

  type Id_t

  type El_t

  type Table_t <: Table[El_t]

  def query: TableQuery[Table_t]

}
