package io.suggest.mbill2.m.common

import java.util.UUID

import io.suggest.mbill2.m.gid.{GidSlick, IdSlick}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 22:33
 * Description: Добавить метод insert в статический контейнер
 */
trait InsertOneReturningBase extends ModelContainer with IdSlick {
  import profile.api._

  /** Апдейт значения экземпляра модели новым id. */
  protected def _withId(el: Table_t#TableElementType, id: Id_t): El_t

  /**
    * Инзерт в таблицу одного элемента.
    *
    * @param el Элемент ряда.
    * @return Сохранённый элемент.
    */
  def insertOne(el: El_t): DBIOAction[El_t, NoStream, Effect.Write]
  // TODO Содержимое insertOne() зависит от явного задания типа Id_t, что мешает нормальной абстракции.
}
trait InsertOneReturning extends InsertOneReturningBase with GidSlick {
  import profile.api._
  override def insertOne(el: El_t): DBIOAction[El_t, NoStream, Effect.Write] = {
    (query returning query.map(_.id) into _withId) += el
  }
}


/** Поддержка легкого инзерта нескольких элементов сразу. */
trait InsertManyReturning extends InsertOneReturningBase {

  import profile.api._

  /**
    * Инзерт пачки элементов сразу.
    *
    * @param els Элементы для инзерта.
    * @return Список сохраненных элементов, скорее всего в исходном порядке..
    */
  def insertMany(els: TraversableOnce[El_t]): DBIOAction[Seq[El_t], NoStream, Effect.Write] = {
    val acts = for (el <- els.toIterator) yield {
      insertOne(el)
    }
    DBIO.sequence( acts.toSeq )
  }

}


/** InsertOneReturning для UUID в id-колонке.
  * Непонятно, как правильно абстрагировать шейп, чтобы всё было ок. */
trait InsertUuidOneReturning extends InsertOneReturningBase {
  override type Id_t = UUID
  import profile.api._
  override def insertOne(el: El_t): DBIOAction[El_t, NoStream, Effect.Write] = {
    (query returning query.map(_.id) into _withId) += el
  }
}
