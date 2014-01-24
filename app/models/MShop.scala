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
  val rowParser = get[Pk[Int]]("id") ~ get[Int]("company_id") ~ get[Int]("mart_id") ~ get[String]("name") ~
    get[Option[String]]("inmart_addr") ~ get[DateTime]("date_created") map {
    case id ~ company_id ~ mart_id ~ name ~ inmart_addr ~ date_created =>
      MShop(id=id, company_id=company_id, mart_id=mart_id, name=name, inmart_addr=inmart_addr, date_created=date_created)
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
}


import MShop._

case class MShop(
  company_id      : Int,
  mart_id         : Int,
  var name        : String,
  var inmart_addr : Option[String],
  id              : Pk[Int] = NotAssigned,
  date_created    : DateTime = null
) extends SqlModelSave[MShop] with MCompanySel with MMartSel with CompanyMartsSel {

  /** Добавить в базу текущую запись.
    * @return Новый экземпляр сабжа.
    */
  def saveInsert(implicit c: Connection): MShop = {
    SQL("INSERT INTO shop(company_id, mart_id, name, inmart_addr) VALUES({company_id}, {mart_id}, {name}, {inmart_addr})")
      .on('company_id -> company_id, 'mart_id -> mart_id, 'name -> name, 'inmart_addr -> inmart_addr)
      .executeInsert(rowParser single)
  }

  /** Обновлить в таблице текущую запись.
    * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
    */
  def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE shop SET name={name}, inmart_addr={inmart_addr} WHERE id={id}")
      .on('id -> id, 'name -> name, 'inmart_addr -> inmart_addr)
      .executeUpdate()
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

