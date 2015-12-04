package io.suggest.mbill2.m.common

import io.suggest.mbill2.m.gid.GidModelContainer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 22:33
 * Description: Добавить метод insert в статический контейнер
 */
trait InsertOneReturning extends GidModelContainer {

  import driver.api._

  /** Апдейт значения экземпляра модели новым id. */
  protected def _withId(el: Table_t#TableElementType, id: Id_t): El_t

  def insertOne(el: El_t) = {
    (query returning query.map(_.id) into _withId) += el
  }

}
