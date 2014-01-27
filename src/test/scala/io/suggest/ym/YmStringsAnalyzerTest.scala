package io.suggest.ym

import org.scalatest._
import YmStringsAnalyzer._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.14 16:05
 * Description: Тесты для распознавалки ключевых слов и словосочетаний, используемых в ключах и значениях YML-данных.
 */
class YmStringsAnalyzerTest extends FlatSpec with Matchers {

  private def getF[T](p: Parser[T], analyzer:YmStringsAnalyzer = new YmStringsAnalyzer): String => T = {
    {pn: String =>
      val pn1 = analyzer.normalizeParamName(pn)
      parseAll(p, pn1).get
    }
  }

  "NORM_PARAM_NAME_PARSER" should "detect param names" in {
    import ParamNames._
    val analyzer = new YmStringsAnalyzer
    val f = getF(NORM_PARAM_NAME_PARSER, analyzer)
    // --- сами тесты
    f("Color")            shouldEqual Color
    f("COLOUR")           shouldEqual Color
    f("Цвет")             shouldEqual Color
    f("ЦвЕт")             shouldEqual Color
    f("Size")             shouldEqual Size
    f("Размер")           shouldEqual Size
    f("Пол")              shouldEqual Gender
    f("Gender")           shouldEqual Gender
    f("Возраст")          shouldEqual Age
    f("agE")              shouldEqual Age
    f("материал")         shouldEqual Material
    f("Матерьиал")        shouldEqual Material
    f("MaTeriaLs")        shouldEqual Material
    f("КаПюшон")          shouldEqual Hood
    f("HOOD")             shouldEqual Hood
    f("длинНа")           shouldEqual Length
    f("Длина куртки")     shouldEqual JacketLength
    f("Высота каблука")   shouldEqual HeelHeight
    f("heel HeIght")      shouldEqual HeelHeight
    f("heigth")           shouldEqual Height
    f("вЫсОтА")           shouldEqual Height
    f("Чашка")            shouldEqual Cup
    f("Размер чашки")     shouldEqual Cup
    f("Size of cup")      shouldEqual Cup
    f("Cup")              shouldEqual Cup
    f("Cup size")         shouldEqual Cup
    f("Обхват груди")     shouldEqual Chest
    f("Chest")            shouldEqual Chest
    f("Размер трусов")    shouldEqual PantsSize
    f("Трусов размер")    shouldEqual PantsSize
    f("Pants size")       shouldEqual PantsSize
    f("Size of pant")     shouldEqual PantsSize
    f("Ширина")           shouldEqual Width
    f("Widht")            shouldEqual Width
    f("Объем")            shouldEqual Volume
    f("Volume")           shouldEqual Volume
    f("Вес")              shouldEqual Weight
    f("Weigth")           shouldEqual Weight
    f("Масса")            shouldEqual Weight
    f("Mass")             shouldEqual Weight
    f("Рост")             shouldEqual Growth
    f("Growht")           shouldEqual Growth
    f("Waist")            shouldEqual Waist
    f("Waistline")        shouldEqual Waist
    f("Талия")            shouldEqual Waist
    f("Обхват талии")     shouldEqual Waist
    f("Sleeve")           shouldEqual Sleeve
    f("Рукав")            shouldEqual Sleeve
    f("Длина рукава")     shouldEqual Sleeve
    f("Sleeve length")    shouldEqual Sleeve
    f("Cuff")             shouldEqual Cuff
    f("Манжеты")          shouldEqual Cuff
    f("Shoulder")         shouldEqual Shoulder
    f("Ширина плеч")      shouldEqual Shoulder
    f("Плечо")            shouldEqual Shoulder
  }

  "WEIGHT_NORM_UNITS_PARSER" should "understand standard units of mass measurments" in {
    import MassUnits._
    val f = getF(MASS_NORM_UNITS_PARSER)
    f("КГ.")              shouldEqual kg
    f("килограммов")      shouldEqual kg
    f("г")                shouldEqual gramm
    f("гр.")              shouldEqual gramm
    f("grams")            shouldEqual gramm
    f("ц.")               shouldEqual centner
    f("centner")          shouldEqual centner
    f("ct.")              shouldEqual carat
    f("кар.")             shouldEqual carat
    f("караты")           shouldEqual carat
    f("lbs")              shouldEqual lbs
    f("pounds")           shouldEqual lbs
    f("фунтов")           shouldEqual lbs
  }

}
