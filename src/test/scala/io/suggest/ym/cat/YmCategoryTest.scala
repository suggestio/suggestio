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

  "CAT_TREE" should "contain top-level categories" in {
    CAT_TREE contains "Музыка и видеофильмы"      shouldBe  true
    CAT_TREE contains "Товары для детей"          shouldBe  true
    CAT_TREE contains "123123"                    shouldBe  false
    CAT_TREE contains "Компьютеры"                shouldBe  true
    CAT_TREE contains "Подарки, сувениры, цветы"  shouldBe  true
    CAT_TREE contains "Услуги"                    shouldBe  true
    CAT_TREE contains "Все для офиса"             shouldBe  true
    CAT_TREE contains "Мебель"                    shouldBe  true
  }

  it should "contain at least some subcategories" in {
    // Если всё правильно, то экзепшенов не будет. Поэтому тут без всяких матчеров
    CAT_TREE / "Музыка и видеофильмы" / "Видеофильмы"
    CAT_TREE / "Животные и растения" / "Растения" / "Декоративно-цветущие растения"
    CAT_TREE / "Животные и растения" / "Аквариумистика" / "Инвентарь для обслуживания аквариумов"
    CAT_TREE / "Товары для здоровья" / "Аптека" / "Медицинские приборы и изделия" / "Диагностическое оборудование" / "Жироанализаторы"
    (CAT_TREE / "Оборудование" / "Строительное оборудование" / "Краны").subcatsOpt shouldBe None
  }

  it should "provide valid levels" in {
    (CAT_TREE / "Музыка и видеофильмы").level shouldEqual 1
    CAT_TREE.level shouldEqual 0
    (CAT_TREE / "Животные и растения" / "Растения" / "Декоративно-цветущие растения").level shouldEqual 3
  }

}
