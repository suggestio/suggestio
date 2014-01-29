package io.suggest.ym.parsers

import scala.util.parsing.combinator.JavaTokenParsers
import io.suggest.ym.{TextNormalizerAn, ParamNames}
import ParamNames.ParamName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.14 16:11
 * Description: Утиль и плагин для парсинга тега param аттрибута name="???".
 */
object ParamNameParser extends JavaTokenParsers {

  /** Парсер нормализованных названий параметров. Нормализация идёт через экземпляр YmStringsAnalyzer. */
  val NORM_PARAM_NAME_PARSER: Parser[ParamName] = {
    import ParamNames._
    // Одежда
    val color      = ("[ck]ol+ou?r".r | "цвет") ^^^ Color
    // Размер
    val sizeW      = "size" | "размер"
    val size       = sizeW ^^^ Size
    // Пол, возраст
    val gender     = ("gender" | "sex" | "пол") ^^^ Gender
    val age        = ("age" | "возраст") ^^^ Age
    val material   = ("materi" | "мат[еи]рь?и?[ая]л?".r) ^^^ Material
    // Капюшон
    val hood       = ("(hoo+d|cowl|tipp?et|c[ao]put?ch)".r | "к[ао]п[юуи]ш[оёе]н+".r) ^^^ Hood
    // Длина
    val lengthW    = "leng[th]+".r | "длин"
    val length     = lengthW ^^^ Length
    // высота
    val heightW    = "heig[th]+".r | "высот"
    val height     = heightW ^^^ Height
    // Каблук
    val heelW      = ("heel" <~ opt("piece")) | "каблук"
    val heelHeight = ((heelW <~ heightW) | (heightW ~> heelW)) ^^^ HeelHeight
    // Размер куртки
    val jacketW    = "ja[ck]+et".r | "куртк"
    val jacketLen  = ((jacketW <~ lengthW) | (lengthW ~> jacketW)) ^^^ JacketLength
    // Вес, рост
    val weight     = ("(weig[ht]+|mas+)".r | "вес" | "масс") ^^^ Weight
    val growth     = ("grou?w[th]+".r | "рост") ^^^ Growth
    // Талия
    val obhvatW: Parser[String] = "обхват"
    val taliaW: Parser[String]  = "тал"
    val waist      = ("wai(st|ts?)(line?)?".r | (opt(obhvatW) ~> taliaW)) ^^^ Waist
    // ширина
    val widthW     = "wid[th]+".r | "ширин"
    val width      = widthW ^^^ Width
    // Обхват груди
    val chestW     = "chest" | "bust" | "груд"
    val chest      = (opt(obhvatW) ~> chestW) ^^^ Chest
    // Рукав
    val sleevW     = "sle+ve?".r | "рукав?".r
    val sleeve     = ((lengthW ~> sleevW) | (sleevW <~ opt(lengthW))) ^^^ Sleeve
    // Плечо
    val shoulderW  = "shou?lders?".r | "плеч"
    val shoulder   = (shoulderW | (widthW ~> shoulderW)) ^^^ Shoulder
    // Манжет
    val cuff       = ("cuf+".r | "м[ао]нж[еэ]т+".r) ^^^ Cuff
    // Одежда: нижнее бельё
    val cupW       = "cup" | "чаш(еч?)?к+".r
    val cupSize    = ((sizeW ~> cupW) | (cupW <~ sizeW) | cupW) ^^^ Cup
    val pantsW     = "(underp(ant)?|pant)".r | "трус"
    val pantsSize  = ((sizeW ~> pantsW) | (pantsW ~> sizeW)) ^^^ PantsSize
    // Объём.
    val volumeW    = "vol(um)?".r | "об([ъь]([её]м)?)?".r    // TODO Есть серьезные проблемы с нормализацией слова "объем" во всех стеммерах.
    val volume     = volumeW ^^^ Volume
    // TODO Надо описать больше параметров вместе с тестами.
    color | gender | material | hood |
      jacketLen | sleeve | cuff | shoulder |
      heelHeight | waist |
      cupSize | chest | pantsSize |
      size | width | length | height | weight | growth | volume | age
  }

}


import ParamNameParser._

trait ParamNameParserAn extends TextNormalizerAn {
  /** Нормализовать и попытаться распарсить значение имени параметра. */
  def parseParamName(pn: String) = {
    val pnNorm = normalizeText(pn)
    parse(NORM_PARAM_NAME_PARSER, pnNorm)
  }
}

