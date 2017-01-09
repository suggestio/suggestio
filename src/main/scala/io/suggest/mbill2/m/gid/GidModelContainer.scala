package io.suggest.mbill2.m.gid

import io.suggest.mbill2.m.common.ModelContainer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 23:06
 * Description: Частоиспользуемое расширение [[io.suggest.mbill2.m.common.ModelContainer]] с Gid_t.
 */
trait GidModelContainer extends ModelContainer with GidSlick {

  import driver.api._

  override type Id_t = Gid_t

  override type Table_t <: Table[El_t] with GidColumn

}
