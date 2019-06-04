package io.suggest.mbill2.m.gid

import io.suggest.mbill2.m.common.ModelContainer
import slick.sql.SqlAction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 22:19
 * Description: Добавить метод чтения по id.
 */
trait GetById extends ModelContainer with GidSlick {

  import profile.api._

  /**
   * Чтение ряда по id.
   * @param id id ряда.
   * @return Slick-экшен получения ряда.
   */
  def getById(id: Id_t): SqlAction[Option[El_t], NoStream, Effect.Read] = {
    query
      .filter(_.id === id)
      .result
      .headOption
  }

}
