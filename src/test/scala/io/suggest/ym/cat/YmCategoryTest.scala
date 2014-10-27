package io.suggest.ym.cat

import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.14 10:22
 * Description: Тесты для системы категорий suggest.io.
 */
class YmCategoryTest extends FlatSpec with Matchers {
  import YmCategory._

  "CAT_TREE_CORE" should "contain top-level categories" in {
    CAT_TREE_CORE contains "123123"                    shouldBe  false
    CAT_TREE_CORE contains "Компьютеры"                shouldBe  true
    CAT_TREE_CORE contains "Подарки и цветы"           shouldBe  true
    CAT_TREE_CORE contains "Услуги"                    shouldBe  true
    CAT_TREE_CORE contains "Все для офиса"             shouldBe  true
  }

  it should "contain at least some subcategories" in {
    // TODO Проверять подкатегории
    // Если всё правильно, то экзепшенов не будет. Поэтому тут без всяких матчеров
    //CAT_TREE_CORE / "Музыка и видеофильмы" / "Видеофильмы"
    //CAT_TREE_CORE / "Животные и растения" / "Растения" / "Декоративно-цветущие растения"
    //CAT_TREE_CORE / "Животные и растения" / "Аквариумистика" / "Инвентарь для обслуживания аквариумов"
    //CAT_TREE_CORE / "Товары для здоровья" / "Аптека" / "Медицинские приборы и изделия" / "Диагностическое оборудование" / "Жироанализаторы"
    //(CAT_TREE_CORE / "Оборудование" / "Строительное оборудование" / "Краны").subcatsOpt shouldBe None
    pending
  }

  it should "provide valid levels" in {
    CAT_TREE_CORE.level shouldEqual 0
    //(CAT_TREE_CORE / "Музыка и видеофильмы").level shouldEqual 1
    // TODO Проверять уровни подкатегорий
  }

}
