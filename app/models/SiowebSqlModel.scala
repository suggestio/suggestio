package models

import anorm._
import java.sql.Connection
import util.AnormPgArray._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 10:13
 * Description: Общий код для sql-моделей sioweb.
 */
trait SiowebSqlModelStatic[T] {

  /** Парсер ряда. */
  val rowParser: RowParser[T]

  /** Название таблицы. Используется при сборке sql-запросов. */
  val TABLE_NAME: String

  /**
   * Прочитать ряд по ключу ряда.
   * @param id id ряда.
   * @return
   */
  def getById(id: Int)(implicit c: Connection): Option[T] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE id = {id}")
      .on('id -> id)
      .as(rowParser *)
      .headOption
  }

  /**
   * Удалить ряд по ключу.
   * @param id id ряда.
   * @return Кол-во удалённых рядов, т.е. 0 или 1.
   */
  def deleteById(id: Int)(implicit c: Connection): Int = {
    SQL("DELETE FROM " + TABLE_NAME + " WHERE id = {id}")
      .on('id -> id)
        .executeUpdate()
  }


  /**
   * Получение пачкой всех перечисленных рядов.
   * @param ids Список id рядов.
   * @return Список результатов в неопределённом порядке.
   */
  def multigetByIds(ids: Seq[Int])(implicit c: Connection): List[T] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE id = ANY({ids})")
      .on('ids -> seqInt2pgArray(ids))
      .as(rowParser *)
  }
}
