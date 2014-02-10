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

  val isExistParser = get[Boolean]("is_exist")

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


  /**
   * Узнать, существует ли компания с указанным id.
   * @param id id компании.
   * @return true, если есть компания, т.е. есть ряд с таким id. Иначе false.
   */
  def isExists(id: Int)(implicit c:Connection): Boolean = {
    SQL("SELECT count(*) > 0 AS is_exist FROM company WHERE id = {id}")
      .on('id -> id)
      .as(isExistParser single)
  }


  /** Выдать все компании, зареганные в системе. Полезно при администрировании. */
  def allById(implicit c:Connection): List[MCompany] = {
    SQL("SELECT * FROM company ORDER BY id DESC")
      .as(rowParser *)
  }


  /**
   * Удалить ряд из таблицы по id.
   * @param companyId Номер ряда компании.
   * @return Кол-во удалённых рядов, т.е. 0 или 1.
   */
  def deleteById(companyId: Int)(implicit c:Connection): Int = {
    SQL("DELETE FROM company WHERE id = {company_id}")
      .on('company_id -> companyId)
      .executeUpdate()
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
      .on('name -> name, 'id -> id.get)
      .executeUpdate()
  }

  /**
   * Удалить текущую запись из таблицы, если она там есть.
   * @return Кол-во удалённых рядов: 0 или 1.
   */
  def delete(implicit c:Connection) = {
    id match {
      case Id(_id)      => deleteById(_id)
      case NotAssigned  => 0
    }
  }
}


trait MCompanySel {
  def company_id: Int
  def company(implicit c: Connection) = getById(company_id).get
}
