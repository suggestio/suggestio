package io.suggest.mbill2.m.gid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 23:03
 * Description: Поддержка метода deleteById().
 */
trait DeleteById extends GidModelContainer {

  import profile.api._

  /**
   * Удалить ряд по id.
   * @param id Ключ ряда.
   * @return
   */
  def deleteById(id: Id_t) = {
    query
      .filter(_.id === id)
      .delete
  }

}
