package models

import anorm._, SqlParser._
import util.AnormJodaTime._
import org.joda.time.DateTime
import util.SqlModelSave
import java.sql.Connection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 19:23
 * Description: Таблица с магазинами, зарегистрированными в системе.
 */

object MShop {

  /** Парсер полного ряда таблицы. */
  val rowParser = get[Pk[Int]]("id") ~ get[Int]("company_id") ~ get[Int]("mart_id") ~ get[String]("name") ~ get[DateTime]("date_created") ~
    get[Option[String]]("description") ~ get[Option[Int]]("mart_floor") ~ get[Option[Int]]("mart_section") map {
    case id ~ company_id ~ mart_id ~ name ~ date_created ~ description ~ mart_floor ~ mart_section =>
      MShop(id=id, company_id=company_id, mart_id=mart_id, name=name, date_created=date_created,
            description=description, mart_floor=mart_floor, mart_section=mart_section)
  }

  /**
   * Выбрать ряд из таблицы по id.
   * @param id Номер ряда.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: Int)(implicit c:Connection): Option[MShop] = {
    SQL("SELECT * FROM shop WHERE id = {id}")
      .on('id -> id)
      .as(rowParser *)
      .headOption
  }

  /**
   * Найти все магазины, относящиеся к указанному ТЦ.
   * @param mart_id id торгового центра.
   * @return Список MShop в неопределённом порядке.
   */
  def getByMartId(mart_id: Int)(implicit c:Connection): List[MShop] = {
    SQL("SELECT * FROM shop WHERE mart_id = {mart_id}")
      .on('mart_id -> mart_id)
      .as(rowParser *)
  }

  /**
   * Выдать все магазины, находящиеся во владении указанной компании.
   * @param company_id id компании.
   * @return Список MShop в неопределённом порядке.
   */
  def getByCompanyId(company_id: Int)(implicit c:Connection): List[MShop] = {
    SQL("SELECT * FROM shop WHERE company_id = {company_id}")
      .on('company_id -> company_id)
      .as(rowParser *)
  }

  /**
   * Найти все магазины, принадлежащие конторе и расположенные в указанном ТЦ.
   * @param company_id id компании-владельца магазина.
   * @param mart_id id торгового центра.
   * @return Список MShop в неопределённом порядке.
   */
  def getByCompanyAndMart(company_id:Int, mart_id:Int)(implicit c:Connection): List[MShop] = {
    SQL("SELECT * FROM shop WHERE company_id = {company_id} AND mart_id = {mart_id}")
      .on('company_id -> company_id, 'mart_id -> mart_id)
      .as(rowParser *)
  }

  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   * @return Список магазинов в порядке их создания.
   */
  def getAll(implicit c: Connection): List[MShop] = {
    SQL("SELECT * FROM shop ORDER BY id ASC")
      .as(rowParser *)
  }

  /**
   * Удалить один ряд из таблицы по id.
   * @param id Ключ ряда.
   * @return Кол-во удалённых рядов. Т.е. 0 или 1.
   */
  def deleteById(id: Int)(implicit c:Connection): Int = {
    SQL("DELETE FROM shop WHERE id = {id}")
      .on('id -> id)
      .executeUpdate()
  }
}


import MShop._

case class MShop(
  var company_id  : Int,
  var mart_id     : Int,
  var name        : String,
  var description : Option[String] = None,
  var mart_floor  : Option[Int] = None,
  var mart_section: Option[Int] = None,
  id              : Pk[Int] = NotAssigned,
  date_created    : DateTime = null
) extends SqlModelSave[MShop] with MCompanySel with MMartSel with CompanyMartsSel {

  /** Добавить в базу текущую запись.
    * @return Новый экземпляр сабжа.
    */
  def saveInsert(implicit c: Connection): MShop = {
    SQL("INSERT INTO shop(company_id, mart_id, name, description, mart_floor, mart_section)" +
        " VALUES({company_id}, {mart_id}, {name}, {description}, {mart_floor}, {mart_section})")
      .on('company_id -> company_id,   'mart_id -> mart_id,       'name -> name,
          'description -> description, 'mart_floor -> mart_floor, 'mart_section -> mart_section)
      .executeInsert(rowParser single)
  }

  /** Обновлить в таблице текущую запись.
    * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
    */
  def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE shop SET company_id={company_id}, mart_id={mart_id}, name={name}, description={description}, mart_floor={mart_floor}, mart_section={mart_section} WHERE id={id}")
      .on('company_id -> company_id, 'mart_id -> mart_id, 'name -> name, 'description -> description, 'mart_floor -> mart_floor, 'mart_section -> mart_section, 'id -> id.get)
      .executeUpdate()
  }


  /** Обновить переменные текущего класса с помощью другого класса.
    * @param newMshop Другой экземпляр MShop, содержащий все необходимые данные.
    */
  def loadFrom(newMshop: MShop) {
    if (!(newMshop eq this)) {
      this.company_id = newMshop.company_id
      this.mart_id = newMshop.mart_id
      this.name = newMshop.name
      this.description = newMshop.description
      this.mart_floor = newMshop.mart_floor
      this.mart_section = newMshop.mart_section
    }
  }

  /** Удалить текущий ряд из таблицы. Если ключ не выставлен, то будет 0.
    * @return Кол-во удалённых рядов, т.е. 0 или 1.
    */
  def delete(implicit c:Connection) = id match {
    case NotAssigned  => 0
    case Id(_id)      => deleteById(_id)
  }
}


trait CompanyShopsSel {
  def company_id: Int
  def companyShops(implicit c:Connection) = getByCompanyId(company_id)
}

trait MartShopsSel {
  def mart_id: Int
  def martShops(implicit c:Connection) = getByMartId(mart_id)
}

trait MShopSel {
  def shop_id: Int
  def shop(implicit c:Connection) = getById(shop_id)
}

