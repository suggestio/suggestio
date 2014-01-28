package io.suggest.ym

import org.apache.lucene.util.Version.{LUCENE_46 => luceneVsn}
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.standard._
import org.apache.lucene.analysis.core._
import org.tartarus.snowball.ext.{RussianStemmer, EnglishStemmer}
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.snowball.SnowballFilter
import org.apache.lucene.analysis.{Analyzer, TokenStream}
import java.io.Reader
import org.apache.lucene.analysis.util.CharArraySet
import java.util
import scala.collection.JavaConversions._
import io.suggest.ym.ParamNames.ParamName
import scala.util.parsing.combinator.JavaTokenParsers
import io.suggest.ym.MassUnits.MassUnit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.14 14:54
 * Description: Поля param имеют локализованное названия в довольно свободной форме.
 * Нужно применить знания языка и нормализовать эти строковые выхлопы, а затем распарсить.
 */
object YmStringsAnalyzer extends JavaTokenParsers {

  /** Дефолтовые русские и английские стоп-слова содержат лишние слова. Тут отфильтровываем их из исходного списка. */
  private def filterStops(cas: CharArraySet, nonStop:List[String]) = {
    val nonStopCh = nonStop.map(_.toCharArray)
    val stopSetFilterF: PartialFunction[AnyRef, Boolean] = {
      case dfltStopword: Array[Char] =>
        !nonStopCh.exists {
          chsNS  =>  util.Arrays.equals(dfltStopword, chsNS)
        }
    }
    cas.iterator()
      .filter(stopSetFilterF)
      .asInstanceOf[Iterator[Array[Char]]]
  }

  /** Неизменяемое потоко-безопасное множество стоп-слов. Используется при сборке анализатора текстов. */
  val PARAM_TEXT_STOPWORD_SET = {
    val ruNonStopCh = List(
      "все", "только", "когда", "человек", "раз", "жизнь", "два", "другой", "больше", "всего", "всегда"
    )
    val ruStop = filterStops(RussianAnalyzer.getDefaultStopSet, ruNonStopCh)
    val enNonStopCh = List("during", "all")
    // TODO Может использовать english_stop.txt? EnglishAnalyzer использует старые захардкоженные стопы.
    val enStop = filterStops(EnglishAnalyzer.getDefaultStopSet, enNonStopCh)
    val mergedStopCh = ruStop ++ enStop
    val mergedStop = mergedStopCh map { new String(_) }
    CharArraySet.unmodifiableSet(
      StopFilter.makeStopSet(luceneVsn, mergedStop.toArray, false)
    )
  }


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


  /** Парсер единиц измерения массы, которые указываются в теге param аттрибуте unit. */
  val MASS_NORM_UNITS_PARSER: Parser[MassUnit] = {
    import MassUnits._
    val kgP       = ("кг" | "килограм+".r | "kg" | "kilogram+".r) ^^^ kg
    val gP        = ("г(р(амм?)?)?".r | "g(ram+)".r) ^^^ gramm
    val mgP       = ("мг" | "мил+иг(р(ам+)?)?".r | "mg" | "mil+igram+".r) ^^^ mg
    val tonP      = ("т(он+)?".r | "ton+".r) ^^^ ton
    // TODO Выпилить центнер? Это не стандартизированная историческая единица, она зависит от страны употребления.
    val centnerP  = ("ц(ей?нтнер|т)?".r | "quintal" | "[zc]entner".r | "кв[iи]нтал[ъь]?".r) ^^^ centner
    val caratP    = ("кар(ат)?".r | "ct" | "carat") ^^^ carat
    val lbP       = ("lbs?".r | "фунт" | "pound") ^^^ lbs
    kgP | gP | mgP | tonP | centnerP | caratP | lbP
  }

}


import YmStringsAnalyzer._

/** thread-local class для маломусорной lucene-нормализации коротких строчек. */
class YmStringsAnalyzer extends Analyzer(Analyzer.GLOBAL_REUSE_STRATEGY) {

  def createComponents(fieldName: String, reader: Reader): TokenStreamComponents = {
    // Заменить букву ё на е.
    val tokens = new StandardTokenizer(luceneVsn, reader)
    tokens.setMaxTokenLength(15)
    var filtered: TokenStream = new StandardFilter(luceneVsn, tokens)  // https://stackoverflow.com/a/16965481
    filtered = new LowerCaseFilter(luceneVsn, filtered)
    // Выкинуть стоп-слова.
    filtered = new StopFilter(luceneVsn, filtered, PARAM_TEXT_STOPWORD_SET)
    // Стеммеры для отбрасывания окончаний.
    filtered = new SnowballFilter(filtered, new RussianStemmer)
    filtered = new SnowballFilter(filtered, new EnglishStemmer)
    new TokenStreamComponents(tokens, filtered)
  }


  /** Нормализовать имя параметра, заданного в произвольной форме. */
  def normalizeParamName(pn: String): StringBuilder = {
    val readyTokens = new StringBuilder
    val stream = tokenStream(null, pn)
    stream.reset()
    while (stream.incrementToken()) {
      val resultToken = stream.getAttribute(classOf[CharTermAttribute]).toString
      readyTokens append resultToken
    }
    stream.close()
    readyTokens
  }


  /** Нормализовать и попытаться распарсить значение имени параметра. */
  def parseParamName(pn: String) = {
    val pnNorm = normalizeParamName(pn)
    parse(NORM_PARAM_NAME_PARSER, pnNorm)
  }


  /** Распарсить единицы измерения массы. */
  def parseMassUnit(muStr: String) = {
    val muStrNorm = normalizeParamName(muStr)
    parse(MASS_NORM_UNITS_PARSER, muStrNorm)
  }
}


