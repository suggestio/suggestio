package io.suggest.ym.cat

import org.scalatest._
import io.suggest.ym.model.YmShopCategory
import io.suggest.ym.YmStringsAnalyzer
import YmCategory.CAT_TREE

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.14 9:35
 * Description: Тесты для маппера магазинных категорий на реальные.
 */
class YmCatTranslatorTest extends FlatSpec with Matchers {

  "Translator" should "handle 1-level categories from carrida.ru yml price" in {
    // прайс взят из http://carrida.ru/yml/full-market.xml
    // Хелпер для дедубликации кода заливки категорий
    val tr = new YmCatTranslator
    val parentIdOpt = Some("0")
    def l(id:String, name:String) {
      val cat = new YmShopCategory(id=id, name=name, parentIdOpt=parentIdOpt)
      tr.learn(cat)
    }
    // Залить в маппер
    l("448070", "GPS-навигаторы")
    l("445961", "Автоаксеccуары")
    l("446748", "Автоакустика")
    l("448910", "Автоантенны")
    l("450874", "Автокресла")
    l("446968", "Автомагнитолы")
    l("448095", "Автомобильные колонки")
    l("451162", "Автомойки")
    l("449028", "Автомониторы")
    l("444691", "Автосабвуферы")
    l("446931", "Автоусилители")
    l("462017", "Автохимия и автокосметика")
    l("451963", "Автохолодильники")
    l("469472", "Алкотестеры")
    l("441758", "Бытовая техника")
    l("450040", "Видеорегистраторы")
    l("464188", "Водоочистка")
    l("465595", "Грузовые автомобили")
    l("445362", "Грузовые диски")
    l("440808", "Грузовые шины")
    l("440745", "Диски")
    l("470699", "Диски для квадроциклов")
    l("469514", "Зарядные и пускозаряд. устр-ва")
    l("463607", "Комплектующие")
    l("469596", "Компрессоры")
    l("453047", "Крепеж")
    l("455123", "Моторное масло")
    l("441080", "Мотошины")
    l("469379", "Пакеты")
    l("469410", "Радар-детекторы")
    l("463779", "Стойки для шин и дисков")
    l("465638", "Транспортные компании")
    l("454172", "Центровочные кольца")
    l("464559", "Чехлы")
    l("440681", "Шины")
    l("470452", "Шины для квадроциклов")

    // Больше категорий в прайсе на момент написания теста не было.
    // Пора получить результат и протестить его:
    implicit val an = new YmStringsAnalyzer
    val r = tr.getResultMap
    r("448070")  shouldBe  CAT_TREE / "Электроника и Фото" / "Навигационное оборудование" / "GPS-навигаторы"
    r("440745")  shouldBe  CAT_TREE / "Авто, мото" / "Колесные диски"
    r("445961")  shouldBe  CAT_TREE / "Авто, мото" / "Аксессуары"
    //r("451162")  shouldBe  CAT_TREE / "Все для дома и дачи" / "Дача, сад и огород" / "Мойки высокого давления"
    /* Автомойки */
  }

}
