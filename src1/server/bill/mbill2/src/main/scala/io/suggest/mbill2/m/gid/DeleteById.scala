package io.suggest.mbill2.m.gid

import io.suggest.mbill2.m.common.ModelContainer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 23:03
 * Description: Поддержка метода deleteById().
 */
trait DeleteById extends ModelContainer with GidSlick {

  import profile.api._

  /**
   * Удалить ряд по id.
   * @param ids Ключ ряда.
   * @return Кол-во выпиленных рядов.
   */
  def deleteById(ids: Id_t*): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .filter(_.id inSet ids)
      .delete
  }

}
