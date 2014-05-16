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
   * @param policy Для управления блокировками ряда можно задать политику.
   * @return
   */
  def getById(id: Int, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): Option[T] = {
    val req = new StringBuilder(50, "SELECT * FROM ")
      .append(TABLE_NAME)
      .append(" WHERE id = {id}")
    policy.append2sb(req)
    SQL(req.toString())
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

  /** Прочитать всю таблицу. */
  def getAll(implicit c: Connection): List[T] = {
    SQL("SELECT * FROM " + TABLE_NAME)
      .as(rowParser *)
  }
}


trait SiowebSqlModel[T] {
  def id: Pk[Int]
  def companion: SiowebSqlModelStatic[T]

  def delete(implicit c: Connection) = id match {
    case Id(_id)  => companion.deleteById(_id)
    case _        => 0
  }
}


/** Бывает необходимо блокировать ряд. Для этого используются SELECT FOR UPDATE, SELECT FOR SHARE.
  * Эти блокировки задаются через параметры к getById(). Следует помнить, что блокировка вызывает акт
  * записи при чтении. */
object SelectPolicies extends Enumeration {
  /** Класс значения. */
  protected abstract case class Val(name: String) extends super.Val(name) {
    /** При построении запроса через StringBuilder используется сие: */
    def append2sb(sb: StringBuilder): StringBuilder
  }

  protected class WithLock(name: String) extends Val(name) {
    override def append2sb(sb: StringBuilder): StringBuilder = {
      sb.append(" FOR ").append(name)
    }
  }

  type SelectPolicy = Val

  val NONE: SelectPolicy = new Val("NONE") {
    // Если нет политики, то и проблем нет.
    override def append2sb(sb: StringBuilder) = sb
  }

  val UPDATE: SelectPolicy = new WithLock("UPDATE")
  val SHARE: SelectPolicy  = new WithLock("SHARE")

  implicit def value2val(x: Value) = x.asInstanceOf[SelectPolicy]
}


/** Утиль, относящаяся к управлению транзакциями. */
object PgTransaction {

  def savepoint(name: String)(implicit c: Connection) {
    SQL("SAVEPOINT " + name).execute()
  }

  def rollbackTo(name: String)(implicit c: Connection) {
    SQL("ROLLBACK TO " + name).execute()
  }

}
