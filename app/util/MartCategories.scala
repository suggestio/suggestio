package util

import models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.14 17:03
 * Description: Генератор дерева категорий для ТЦ и прочая утиль для этого.
 */
object MartCategories {

  /** Отправить в хранилище дефолтовое для ТЦ дерево категорий. */
  def saveDefaultMartCatsFor(adnId: String): Future[Seq[String]] = {
    val catSaveFuts: Seq[Future[String]] = Seq(
      MMartCategory(
        name = "Одежда",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "a", inherit = false),
        parentId = None,
        position = 10,
        cssClass = Some("sm-clothing"),
        includeInAll = true
      ).save,
      MMartCategory(
        name = "Обувь",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "a5", inherit = true),
        parentId = None,
        position = 20,
        cssClass = Some("sm-boots"),
        includeInAll = true
      ).save,
      MMartCategory(
        name = "Туризм и отдых",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "d", inherit = true),
        parentId = None,
        position = 30,
        cssClass = Some("sm-travel"),
        includeInAll = false
      ).save,
      MMartCategory(
        name = "Электроника",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "h", inherit = true),
        parentId = None,
        position = 40,
        cssClass = Some("sm-electronics"),
        includeInAll = true
      ).save,
      MMartCategory(
        name = "Спорттовары",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "e", inherit = true),
        parentId = None,
        position = 50,
        cssClass = Some("sm-sports"),
        includeInAll = true
      ).save,
      MMartCategory(
        name = "Косметика",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "a3", inherit = true),
        parentId = None,
        position = 60,
        cssClass = Some("sm-cosmetics"),
        includeInAll = true
      ).save,
      MMartCategory(
        name = "Товары для дома",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "5", inherit = true),
        parentId = None,
        position = 70,
        cssClass = Some("sm-for-home"),
        includeInAll = true
      ).save,
      MMartCategory(
        name = "Кафе и рестораны",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "6", inherit = false),
        parentId = None,
        position = 80,
        cssClass = Some("sm-restraunts"),
        includeInAll = true
      ).save,
      MMartCategory(
        name = "Гастрономия",
        ownerId = adnId,
        ymCatPtr = MMartYmCatPtr(ycId = "c", inherit = true),
        parentId = None,
        position = 90,
        cssClass = Some("sm-food"),
        includeInAll = true
      ).save
    )
    Future.sequence(catSaveFuts)
  }

}
