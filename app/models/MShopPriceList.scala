package models

import anorm._, SqlParser._
import io.suggest.ym.model.AuthInfoDatum
import util.SqlModelSave
import java.sql.Connection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.02.14 18:46
 * Description: Таблица хранит адреса прайс-листов магазинов. Модель является хранилищем для
 * [[io.suggest.ym.model.YmShopPriceUrlDatum]].
 */

object MShopPriceList {

  /** Разделитель имени и пароля в строке auth_info. */
  val AUTH_INFO_SEP = ":"

  /** Парсер колонки auth_info, которая содержит комплексное значение. */
  val authInfoParser = {
    get[Option[String]]("auth_info") map {
      case None     => None
      case Some(s)  =>
        val Array(username, pw) = s.split("::", 2)
        Some(UsernamePw(username, pw))
    }
  }

  /** Парсер ряда из таблицы. */
  val rowParser = get[Pk[Int]]("id") ~ get[Int]("shop_id") ~ get[String]("url") ~ authInfoParser map {
    case id ~ shop_id ~ url ~ auth_info =>
      MShopPriceList(id=id, shop_id=shop_id, url=url, auth_info=auth_info)
  }


  /**
   * Прочитать ряд по ключу (по id).
   * @param id Номер ряда.
   * @return Распарсенный ряд, если найден. 
   */
  def getById(id: Int)(implicit c:Connection): Option[MShopPriceList] = {
    SQL("SELECT * FROM shop_pricelist WHERE id = {id}")
      .on('id -> id)
      .as(rowParser *)
      .headOption
  }


  /**
   * Прочитать все прайс-листы, относящиеся к указанному магазину.
   * @param shopId id магазина.
   * @return Список прайслистов, относящихся к магазину.
   */
  def getForShop(shopId: Int)(implicit c:Connection): List[MShopPriceList] = {
    SQL("SELECT * FROM shop_pricelist WHERE shop_id = {shop_id}")
      .on('shop_id -> shopId)
      .as(rowParser *)
  }


  /**
   * Удалить ряд прайс-листа по ключу.
   * @param id Ключ ряда.
   * @return Кол-во удалённых рядов, т.е. 0 или 1.
   */
  def deleteById(id: Int)(implicit c: Connection): Int = {
    SQL("DELETE FROM shop_pricelist WHERE id = {id}")
      .on('id -> id)
      .executeUpdate()
  }
}


import MShopPriceList._

case class MShopPriceList(
  shop_id   : Int,
  url       : String,
  auth_info : Option[UsernamePw],
  id        : Pk[Int] = NotAssigned
) extends SqlModelSave[MShopPriceList] with MShopSel {

  def authInfoStr: Option[String] = auth_info map { _.serialize }

  /** Добавить в базу текущую запись.
    * @return Новый экземпляр сабжа.
    */
  def saveInsert(implicit c: Connection): MShopPriceList = {
    SQL("INSERT INTO shop_pricelist(shop_id, url, auth_info) VALUES ({shop_id}, {url}, {auth_info})")
      .on('shop_id -> shop_id, 'url -> url, 'auth_info -> authInfoStr)
      .executeInsert(rowParser single)
  }

  /** Обновлить в таблице текущую запись.
    * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
    */
  def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE shop_pricelist SET url = {url}, auth_info = {auth_info} WHERE id = {id}")
      .on('id -> id, 'url -> url, 'auth_info -> auth_info)
      .executeUpdate()
  }

  /** Удалить текущий ряд из базы, если он там есть. Вернуть кол-во удалённых рядов. */
  def delete(implicit c:Connection) = id match {
    case NotAssigned => None
    case Id(_id)     => deleteById(_id)
  }
}

/** Легковесное представление пары UsernamePw для распарсенного значения колонки auth_info.
  * @param username Имя пользователя. Нельзя, чтобы в имени содержался [[models.MShopPriceList.AUTH_INFO_SEP]].
  * @param password Пароль.
  */
case class UsernamePw(username: String, password: String) {
  def toDatum = new AuthInfoDatum(username=username, password=password)
  def serialize = username + AUTH_INFO_SEP + password
}

trait ShopPriceListSel {
  def shop_id: Int
  def priceLists(implicit c: Connection) = getForShop(shop_id)
}

