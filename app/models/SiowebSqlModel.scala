package models

import anorm._
import java.sql.Connection
import util.anorm.AnormPgArray
import AnormPgArray._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 10:13
 * Description: Общий код для sql-моделей sioweb.
 */

object SqlModelStatic {
  import SqlParser._

  val boolColumnParser = get[Boolean]("bool")
}


trait SqlTableName {

  /** Название таблицы. Используется при сборке sql-запросов. */
  val TABLE_NAME: String

  /** Блокировка таблицы на запись в целях защиты от добавления рядов. */
  def lockTableWrite(implicit c: Connection) {
    SQL("LOCK TABLE " + TABLE_NAME + " IN ROW EXCLUSIVE MODE")
      .executeUpdate()
  }

  def hasId(id: Int)(implicit c: Connection): Boolean = {
    SQL("SELECT count(*) > 0 AS bool FROM " + TABLE_NAME + " WHERE id = {id}")
      .on('id -> id)
      .as(SqlModelStatic.boolColumnParser single)
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

}


trait SqlModelStaticMinimal extends SqlTableName {

  type T

  /** Парсер одного ряда. */
  val rowParser: RowParser[T]


  /** Прочитать всю таблицу. */
  def getAll(implicit c: Connection): List[T] = {
    SQL("SELECT * FROM " + TABLE_NAME)
      .as(rowParser *)
  }

  /** Произвольный поиск с доступом к блокировкам.
    * Генерится запрос на базе "select * from ..." и отправляется на исполнение.
    * @param afterFrom SQL-часть запроса, идущая после "FROM %TABLE_NAME%". В частности пустая строка.
    * @param policy Политика блокировки.
    * @param args Аргументы SQL-запроса. Тоже самое, что и в SQL.on().
    * @return Список экземпляров модели в произвольном либо заданном порядке.
    */
  def findBy(afterFrom: String, policy: SelectPolicy, args: NamedParameter*)(implicit c: Connection) = {
    val sb = new StringBuilder("SELECT * FROM ").append(TABLE_NAME).append(afterFrom)
    policy.append2sb(sb)
    SQL(sb.toString())
      .on(args: _*)
      .as(rowParser *)
  }

}


trait SqlModelStatic extends SqlModelStaticMinimal {

  /**
   * Прочитать ряд по ключу ряда.
   * @param id id ряда.
   * @param policy Для управления блокировками ряда можно задать политику.
   * @return
   */
  def getById(id: Int, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): Option[T] = {
    getByIdBase(id, policy, None)
  }

  protected def getByIdBase(id: Int, policy: SelectPolicy = SelectPolicies.NONE, whereAppendix: Option[String] = None)
                           (implicit c: Connection): Option[T] = {
    val req = new StringBuilder(50, "SELECT * FROM ")
      .append(TABLE_NAME)
      .append(" WHERE id = {id}")
    if (whereAppendix.isDefined)
      req.append(' ').append(whereAppendix.get)
    policy.append2sb(req)
    SQL(req.toString())
      .on('id -> id)
      .as(rowParser *)
      .headOption
  }

  /**
   * Получение пачкой всех перечисленных рядов.
   * @param ids Список id рядов.
   * @return Список результатов в неопределённом порядке.
   */
  def multigetByIds(ids: Seq[Int])(implicit c: Connection): List[T] = {
    if (ids.isEmpty) {
      Nil
    } else {
      SQL("SELECT * FROM " + TABLE_NAME + " WHERE id = ANY({ids})")
        .on('ids -> seqInt2pgArray(ids))
        .as(rowParser *)
    }
  }

}


trait SqlModelDelete {
  def id: Option[Int]
  def companion: SqlModelStatic

  def delete(implicit c: Connection) = {
    if (id.isDefined) {
      companion.deleteById(id.get)
    } else {
      0
    }
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
    SQL("SAVEPOINT " + name).on().execute()
  }

  def rollbackTo(name: String)(implicit c: Connection) {
    SQL("ROLLBACK TO " + name).on().execute()
  }

}

trait FromJson {
  type T
  def fromJson: PartialFunction[AnyRef, T]
}


/** Аддон для Static-модели, добавляющий метод быстрой для очистки всей таблицы. */
trait SqlTruncate extends SqlTableName {
  def truncateTable(implicit c: Connection): Int = {
    SQL("TRUNCATE TABLE " + TABLE_NAME)
      .executeUpdate()
  }
}


/** Добавить метод analyze() для статической модели. */
trait SqlAnalyze extends SqlTableName {
  def analyze(implicit c: Connection) {
    SQL("ANALYZE " + TABLE_NAME)
      .on()
      .execute()
  }
}

trait SqlVacuumAnalyze extends SqlTableName {
  def vacuumAnalyze(implicit c: Connection): Unit = {
    SQL("VACUUM ANALYZE " + TABLE_NAME)
      .on()
      .execute()
  }
}

trait SqlIndexName extends SqlTableName {

  /** Подготовить имя индекса для указанной колонки.
    * @param colName Имя колонки.
    * @return Имя индекса, которое постоено также, как это делает postgres/pgadmin.
    */
  def indexName(colName: String) = s"${TABLE_NAME}_${colName}_idx"

}

