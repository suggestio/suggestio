package io.suggest.mbill2.m.gid

import io.suggest.mbill2.m.common.ModelContainer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 12:14
  * Description: Аддон для поддержки multiget, т.е. получения сразу пачки рядов по их id.
  */
trait MultiGetById extends ModelContainer with GidSlick {

  import profile.api._

  def getByIds(ids: Iterable[Gid_t]) = {
    query
      .filter(_.id inSet ids)
      .result
  }

}
