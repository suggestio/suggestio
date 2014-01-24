package models

import anorm._, SqlParser._
import org.joda.time.DateTime
import util.AnormJodaTime._
import java.sql.Connection
import util.SqlModelSave

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 17:40
 * Description: Компания - верхний элемент иерархии структуры юр.лиц. Компания может владеть магазинами и торговыми
 * центрами.
 */

object MCompany {

  /** Парсер полного ряда, выделенного через "SELECT * FROM company ...". */
  val rowParser = get[Pk[Int]]("id") ~ get[String]("name") ~ get[DateTime]("date_created") map {
    case id ~ name ~ date_created  =>  MCompany(id=id, name=name, date_created=date_created)
  }

  /**
   * Прочитать из БД одну контору.
   * @param id id'шник компании.
   * @return Распарсенный ряд компании, если найден.
   */
  def getById(id: Int)(implicit c:Connection): Option[MCompany] = {
    SQL("SELECT * FROM company WHERE id = {id}")
      .on('id -> id)
      .as(rowParser *)
      .headOption
  }

}


import MCompany._

/**
 * Экземпляр распарсенного ряда БД.
 * @param name Название конторы.
 * @param id id по базе.
 * @param date_created Дата создания ряда/компании.
 */
case class MCompany(
  var name      : String,
  id            : Pk[Int] = NotAssigned,
  date_created  : DateTime = null
) extends SqlModelSave[MCompany] with CompanyShopsSel with CompanyMartsSel {
  def company_id = id.get

  /** Добавить в таблицу новую запись. */
  def saveInsert(implicit c: Connection) = {
    SQL("INSERT INTO company(name) VALUES ({name})")
      .on('name -> name)
      .executeInsert(rowParser single)
  }

  /** Обновить в таблице существующую запись. */
  def saveUpdate(implicit c: Connection) = {
    SQL("UPDATE company SET name = {name} WHERE id = {id}")
      .on('name -> name, 'id -> id)
      .executeUpdate()
  }
}


trait MCompanySel {
  def company_id: Int
  def company(implicit c: Connection) = getById(company_id)
}
