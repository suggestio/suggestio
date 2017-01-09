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

  /**
    * Инзерт в таблицу одного элемента.
    *
    * @param el Элемент ряда.
    * @return Сохранённый элемент.
    */
  def insertOne(el: El_t) = {
    (query returning query.map(_.id) into _withId) += el
  }

}


/** Поддержка легкого инзерта нескольких элементов сразу. */
trait InsertManyReturning extends InsertOneReturning {

  import driver.api._

  /**
    * Инзерт пачки элементов сразу.
    *
    * @param els Элементы для инзерта.
    * @return Список сохраненных элементов, скорее всего в исходном порядке..
    */
  def insertMany(els: TraversableOnce[El_t]) = {
    val acts = for (el <- els.toIterator) yield {
      insertOne(el)
    }
    DBIO.sequence( acts.toSeq )
  }

}
