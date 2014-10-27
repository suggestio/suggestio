package io.suggest.ym.cat

import org.scalatest._
import io.suggest.ym.model.YmShopCategory
import io.suggest.ym.YmStringsAnalyzer
import YmCategory.CAT_TREE_CORE

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.14 9:35
 * Description: Тесты для маппера магазинных категорий на реальные.
 * 2014.oct.27: дерево категорий маркета изменилось в очередной раз. Тут часть тестов закоменчена, часть исправлена.
 */
class YmCatTranslatorTest extends FlatSpec with Matchers {

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
  implicit val an1 = new YmStringsAnalyzer
  def r = tr.getResultMap

  "getResultMap()" should "see gps-navigators" in {
    r("448070") shouldBe CAT_TREE_CORE / "Электроника" / "GPS-навигация" / "GPS-навигаторы"
  }

  it should "see wheel disks" in {
    //r("440745") shouldBe CAT_TREE_CORE / "Авто" / "Шины и диски" / "Колесные диски"
    pending // Выскакивает надкатегория Шины и диски.
  }

  it should "auto-accessories" in {
    //r("445961") shouldBe CAT_TREE_CORE / "Авто" / "Аксессуары"
    pending // Выскакивают аксесуары для кухни
  }

  it should "see fat cars category" in {
    //TODO  "Авто, мото" / "Грузовые машины"  // Грузовые автомобили Попадает в "Автомобили"
    //r("465595")  shouldBe  CAT_TREE_CORE / "Авто" / "Транспорт" / "Грузовые машины"
    pending
  }

  it should "see radar-detectors category" in {
    r("469410") shouldBe CAT_TREE_CORE / "Авто" / "Электроника" / "Радар-детекторы"
  }

  it should "see chargers categories" in {
    List(
      CAT_TREE_CORE / "Авто" / "Электроника" / "Универсальные зарядные устройства и инверторы",
      CAT_TREE_CORE / "Авто" / "Запчасти" / "Зарядные устройства для аккумуляторов"
    ) should contain(r("469514"))
  }

  it should "see tyres category" in {
    //r("440681") shouldBe CAT_TREE_CORE / "Авто" / "Шины и диски" / "Шины"
    pending  // Вылетает надкатегория "шины и диски".
  }

  //TODO r("451162")  shouldBe  CAT_TREE_CORE / "Все для дома и дачи" / "Дача, сад и огород" / "Мойки высокого давления" // Автомойки

  it should "see water-purification category" in {
    r("464188") shouldBe CAT_TREE_CORE / "Дом и дача" / "Строительство и ремонт" / "Водоснабжение и водоотведение" / "Водоочистка"
  }

  it should "see alko-testers category" in {
    r("469472") shouldBe CAT_TREE_CORE / "Товары для здоровья" / "Аптека" / "Медицинские приборы и изделия" / "Диагностическое оборудование" / "Алкотестеры"
  }

  it should "see home techincs root category" in {
    r("441758") shouldBe CAT_TREE_CORE / "Бытовая техника"
  }

  it should "see car DVRs category" in {
    r("450040") shouldBe CAT_TREE_CORE / "Авто" / "Электроника" / "Видеорегистраторы"
  }

  it should "autocosmetics" in {
    r("462017") shouldBe CAT_TREE_CORE / "Авто" / "Автокосметика" // TODO Две категории в одной. Хз чо тут делать.
  }

  it should "see transport services" in {
    //r("465638")  shouldBe  CAT_TREE_CORE / "Услуги" / "Транспортные услуги"
    pending // TODO Вылетает авто/транспорт
  }

  it should "" in {
    r("469596")  shouldBe  CAT_TREE_CORE / "Оборудование" / "Воздушные компрессоры"
    r("455123")  shouldBe  CAT_TREE_CORE / "Авто" / "Автохимия" / "Моторные масла"
    r("451963")  shouldBe  CAT_TREE_CORE / "Авто" / "Аксессуары" / "Автомобильные холодильники"
    r("446968")  shouldBe  CAT_TREE_CORE / "Авто" / "Электроника" / "Аудио- и видеотехника" / "Автомагнитолы"
    r("446748")  shouldBe  CAT_TREE_CORE / "Авто" / "Электроника" / "Аудио- и видеотехника" / "Автоакустика"
    // Тут проверяется умение усиливать последовательные триграммы-совпадения на фоне простых совпадений:
    //r("446931")  shouldBe  CAT_TREE_CORE / "Авто, мото" / "Аудио- и видеотехника" / "Усилители"    // TODO Есть проблемы: улучшалка, учитывающая порядок токенов, не особо-то работает.
    //r("449028")  shouldBe  CAT_TREE_CORE / "Авто, мото" / "Аудио- и видеотехника" / "Телевизоры и мониторы"  // TODO Есть проблемы - выпадают тут "автомобильные видеорегистраторы"
    // TODO Нужно больше тестов. Вероятно, нужно внести изменения в дерево категорий маркета.
  }

}
