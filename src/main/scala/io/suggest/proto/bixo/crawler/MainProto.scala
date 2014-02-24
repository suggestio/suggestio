package io.suggest.proto.bixo.crawler

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.11.13 18:10
 * Description: Тут сообщения для общения с main-кравлером.
 */

object MainProto {
  val NAME = "main"

  type MajorRebuildReply_t = Either[String, String]

  type MartId_t = String
  type ShopId_t = String

  // Сигналы об изменении конфигурации магазинной части

}

import MainProto.MartId_t
import io.suggest.proto.bixo.crawler.MainProto.ShopId_t


/** Сообщение о необходимости начать процедуру полного ребилда на ближайшей итерации. */
case object MajorRebuildMsg extends Serializable

/**
 * Веб-морда перекидывает пачку награбленных referrer'ов в main-кравлер.
 * Кравлер должен проверять ссылки на принадлежность к dkeys самостоятельно.
 * @param urls Ссылки на страницы.
 */
case class ReferrersBulk(urls: List[String]) extends Serializable



/*=========================== Магазины и торговые центры ==================================*/

/**
 * Сообщение о добавлении нового торгового центра. Кравлер должен подготовится к добавлению магазинов.
 * @param mart_id id торгового центра.
 */
case class MartAdd(mart_id: MartId_t) extends Serializable
case class MartDelete(mart_id: MartId_t) extends Serializable


/**
 * Сообщение о добавлении нового магазина в торговый центр. Кравлер должен подготовиться к загрузке данных
 * по указанному магазину.
 * @param mart_id ID торг.центра, к которому относится магазин.
 * @param shop_id ID Нового магазина.
 */
case class ShopAdd(mart_id:MartId_t, shop_id:ShopId_t) extends Serializable
case class ShopDelete(mart_id:MartId_t, shop_id:ShopId_t) extends Serializable
