package io.suggest.mbill2.m.gid

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 12:14
  * Description: Аддон для поддержки multiget, т.е. получения сразу пачки рядов по их id.
  */
trait MultiGetById extends GidModelContainer {

  import driver.api._

  def getByIds(ids: Traversable[Gid_t]) = {
    query
      .filter(_.id inSet ids)
      .result
  }

}
