package models

import anorm._, SqlParser._
import util.AnormJodaTime._
import org.joda.time.DateTime
import java.sql.Connection
import util.SqlModelSave

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.14 18:31
 * Description: Mart -- это торговое помещение, имеющее адрес. Частные случаи:
 * - Торговый центр. Как правило, это и есть mart.
 * - Рынок.
 * - Собственное помещение единственного мазагина.
 */

object MMart {

  /** Парсер ряда, выделенного из таблицы через "SELECT * FROM mart" */
  val rowParser = get[Pk[Int]]("id") ~ get[Int]("company_id") ~ get[String]("name") ~ get[String]("address") ~
    get[Option[String]]("site_url") ~ get[DateTime]("date_created") map {
    case id ~ company_id ~ name ~ address ~ site_url ~ date_created =>
      MMart(id=id, company_id=company_id, name=name, address=address, site_url=site_url, date_created=date_created)
  }

  /**
   * Прочитать ряд торгового центра из таблицы, если есть такой.
   * @param id Номер ряда (номер ТЦ).
   * @return Экземпляр MMart, если такой ряд есть.
   */
  def getById(id: Int)(implicit c:Connection): Option[MMart] = {
    SQL("SELECT * FROM mart WHERE id = {id}")
      .on('id -> id)
      .as(rowParser *)
      .headOption
  }

  /**
   * Вернуть все ТЦ, находящиеся во владении указанной конторы.
   * @param company_id id конторы.
   * @return Список ТЦ в неопределённом порядке.
   */
  def getByCompanyId(company_id: Int)(implicit c:Connection): List[MMart] = {
    SQL("SELECT * FROM mart WHERE company_id = {company_id} ORDER BY id ASC")
      .on('company_id -> company_id)
      .as(rowParser *)
  }

  /**
   * Выдать все известные торговые центы. В перспективе -- малополезная функция,
   * т.к. не привязана к географии.
   * @return Список ТЦ в порядке создания, старые сверху.
   */
  def getAll(implicit c:Connection): List[MMart] = {
    SQL("SELECT * FROM mart ORDER BY id ASC")
      .as(rowParser *)
  }
}


import MMart._

case class MMart(
  company_id    : Int,
  var name      : String,
  var address   : String,
  var site_url  : Option[String],
  id            : Pk[Int] = NotAssigned,
  date_created  : DateTime = null
) extends SqlModelSave[MMart] with MCompanySel with CompanyShopsSel with MartShopsSel {
  def mart_id = id.get

  /**
   * Добавить новый ряд в базе.
   * @return Вернуть новый экземпляр сабжа с новыми id и date_created.
   */
  def saveInsert(implicit c: Connection): MMart = {
    SQL("INSERT INTO mart(company_id, name, address, site_url) VALUES({company_id}, {name}, {address}, {site_url})")
      .on('company_id -> company_id, 'name -> name, 'address -> address, 'site_url -> site_url)
      .executeInsert(rowParser single)
  }

  /**
   * Обновить данные по текущему ТЦ в базе.
   * @return Кол-во обновлённых рядов, т.е. 0 или 1.
   */
  def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE mart SET name = {name}, address = {address}, site_url = {site_url} WHERE id = {id}")
      .on('id -> id.get, 'name -> name, 'address -> address, 'site_url -> site_url)
      .executeUpdate()
  }

}


trait MMartSel {
  def mart_id: Int
  def mart(implicit c:Connection) = getById(mart_id).get
}

trait CompanyMartsSel {
  def company_id: Int
  def companyMarts(implicit c:Connection) = getByCompanyId(company_id)
}

