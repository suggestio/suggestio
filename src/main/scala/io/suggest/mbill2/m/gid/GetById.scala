package io.suggest.mbill2.m.gid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 22:19
 * Description: Добавить метод чтения по id.
 */
trait GetById extends GidModelContainer {

  import driver.api._

  /**
   * Чтение ряда по id.
   * @param id id ряда.
   * @return Slick-экшен получения ряда.
   */
  def getById(id: Id_t) = {
    query
      .filter(_.id === id)
      .result
      .headOption
  }

}
