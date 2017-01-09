package util.ai.sax.weather.gidromet

import models.ai._

import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 18:32
 * Description: Утиль для поддержки работы с данными от гидрометцентра.
 */

/** Утиль для парсинга данных от гидромета. */
trait GidrometParsers extends JavaTokenParsers {

  override protected val whiteSpace: Regex = """[.,;\s]+""".r

  def smallP: Parser[_] = "(?U)(небольш|слаб)\\w*".r
  def powerP: Parser[_] = "(?U)сильн\\w*".r

  def dayP: Parser[_] = "(?U)(ден|дн[ёе])\\w*".r
  def nightP: Parser[_] = "(?U)ноч\\w*".r

  def cloudyP: Parser[_] = "(?U)облач\\w*".r
  def variableP: Parser[_] = "(?U)перемен\\w*".r
  def sunnyP: Parser[_] = "(?U)(солнеч|ясн)\\w*".r

  def withP: Parser[_] = "[сc]".r
  def withoutP: Parser[_] = "(?U)без".r
  def andP: Parser[_] = "и"

  def percipationsP: Parser[_] = "(?U)осадк\\w*".r
  def snowP: Parser[_] = "(?U)снег\\w*".r
  def rainP: Parser[_] = "(?U)дожд\\w*".r
  def rainShowerP: Parser[_] = "(?U)ливе?н\\w*".r | (powerP ~> rainP)
  def thunderP: Parser[_] = "(?U)(гро[зм]|молн|шторм|буря)\\w*".r
  def iceP: Parser[_] = "(?U)ледян\\w*".r
  def fogP: Parser[_] = "(?U)туман\\w*".r
  def proyasnP: Parser[_] = "(?U)проясн\\w*".r


  def partlyCloudy = (("(?U)мало?".r | smallP) <~ cloudyP) ^^^ SkyStates.CloudPartly
  def varCloudy = {
    ((variableP <~ cloudyP) | cloudyP ~> withP ~> proyasnP) ^^^ SkyStates.CloudVary
  }
  def cloudy = {
    (cloudyP <~ opt(withoutP ~> proyasnP)) ^^^ SkyStates.Cloudy
  }
  def clearSky = sunnyP ^^^ SkyStates.Clear

  def skyStateP: Parser[SkyState] = partlyCloudy | varCloudy | cloudy | clearSky


  def withoutPercipations = (withoutP ~> percipationsP) ^^^ Precipations.NoPercipations
  def smallRain = (smallP ~> rainP) ^^^ Precipations.SmallRain
  def rain = (rainP | rainShowerP) ^^^ Precipations.Rain
  def iceRain = (iceP ~> rain) ^^^ Precipations.IceRain
  def smallSnow = (smallP ~> snowP) ^^^ Precipations.SmallSnow
  def snow = opt(powerP) ~> snowP ^^^ Precipations.Snow
  def thunder = opt((rainP | rainShowerP) <~ withP) ~> opt(powerP) ~> rep1(thunderP | andP) ^^^ Precipations.Thunder
  def fog = (opt(powerP | smallP) ~> fogP) ^^^ Precipations.Fog

  def precipation: Parser[Precipation] = {
    withoutPercipations | smallRain | iceRain | thunder | rain | smallSnow | snow | fog
  }


  override def decimalNumber: Parser[String] = """(\d+([,.]\d*)?|\d*[.,]\d+)""".r
  def floatNumber = decimalNumber ^^ { _.toFloat }
  def signedFloatNumber = "[+-]?(\\d+([.,]\\d*)?|\\d*[.,]\\d+)".r ^^ { _.toFloat }


  def temperatureP: Parser[_] = "(?U)темпер\\w*".r
  def celsiusP: Parser[_] = "°"

  def temperatures: Parser[Temperatures] = {
    val tempValP = signedFloatNumber <~ opt(celsiusP)
    val temps = Temperatures()
    val nightTemp = (nightP ~> tempValP) ^^ {
      v => temps.nightOpt = Some(v)
    }
    val dayTemp = (dayP ~> tempValP) ^^ {
      v => temps.dayOpt = Some(v)
    }
    (temperatureP ~> opt((nightTemp ~ dayTemp) | dayTemp ~ nightTemp)) ^^^ temps
  }


  def windWordP: Parser[_] = "(?U)вете?р\\w*".r

  def eastP: Parser[_] = "(?U)вост\\w*".r
  def westP: Parser[_] = "(?U)запад\\w*".r
  def northP: Parser[_] = "(?U)север\\w*".r
  def southP: Parser[_] = "(?U)ю(жн|г)\\w*".r

  def geoDirectionP: Parser[GeoDirection] = {
    import GeoDirections._
    val _eastP = eastP
    val _westP = westP
    val delimOpt = opt("[-–‒—―]+".r)
    val e = _eastP ^^^ EAST
    val w = _westP ^^^ WEST
    val nd: Parser[GeoDirection] = northP ~> opt(delimOpt ~> (_eastP ^^^ NORTH_EAST | _westP ^^^ NORTH_WEST)) ^^ {
      _ getOrElse NORTH
    }
    val sd: Parser[GeoDirection] = southP ~> opt(delimOpt ~> (_eastP ^^^ SOUTH_EAST | _westP ^^^ SOUTH_WEST)) ^^ {
      _ getOrElse SOUTH
    }
    nd | sd | e | w
  }

  def speedMs: Parser[Float] = {
    floatNumber <~ """(?U)[мm][/\\]""".r <~ withP
  }

  def windP: Parser[Wind] = {
    windWordP ~> (geoDirectionP ~ speedMs) ^^ {
      case windDirection ~ windSpeed => Wind(windDirection, windSpeed)
    }
  }

  def intNumberP = wholeNumber ^^ { _.toInt }

  // TODO Нужно запилить атм.давление, вероятность осадков и тесты.

  def pressureWordP: Parser[_] = "(?U)атм\\w*".r ~> "(?U)д[ао]влен\\w*".r
  def pressureUnitP: Parser[_] = "[mм]{2}".r ~> "[рp][тt]".r ~> "[cс][тt]".r
  def pressureP(acc: AtmPressure): Parser[AtmPressure] = {
    val nP = intNumberP <~ pressureUnitP
    val dayPressP = (dayP ~> nP) ^^ {
      case p => acc.dayOpt = Some(p)
    }
    val nightPressP = (nightP ~> nP) ^^ {
      case p => acc.nightOpt = Some(p)
    }
    pressureWordP ~> rep1(dayPressP | nightPressP) ^^^ acc
  }


  def percipChanceWordP: Parser[_] = "(?U)вероятн\\w*".r ~> "(?U)осад\\w*".r
  def precipChanceP: Parser[Int] = {
    percipChanceWordP ~> intNumberP <~ "%"
  }

  /** Парсер одного rss-дескипшена росгидрометцентра. */
  def dayWeatherAccP(acc: DayWeatherAcc): Parser[DayWeatherAcc] = {
    val ssp = skyStateP ^^ {
      case ss  =>  acc.skyStateOpt = Some(ss)
    }
    val ppp = precipation ^^ {
      case pp =>  acc.precipations = pp :: acc.precipations
    }
    val tpp = temperatures ^^ {
      case ts =>  acc.temperatures = ts
    }
    val wpp = windP ^^ {
      case wind => acc.windOpt = Some(wind)
    }
    val prp = pressureP(acc.pressureMmHg) ^^ {
      case p => acc.pressureMmHg = p    // TODO Присваивание наверное не нужно делать, т.к. оно в аккамуляторе и так.
    }
    val pccp = precipChanceP ^^ {
      case pc => acc.precipChanceOpt = Some(pc)
    }
    // Наличие мусора вполне возможно. Нужно его пропускать и двигаться дальше.
    val elseP = "(?U)\\w+".r ^^ {
      case p => println("Skipping unknown token: " + p)
    }
    rep(ssp | ppp | tpp | wpp | prp | pccp | elseP) ^^^ acc
  }

  /** Парсер, генерящий из исходной строки финальный bean, пригодный для отправки в scalasti. */
  def dayWeatherP(acc: DayWeatherAcc): Parser[DayWeatherBean] = {
    dayWeatherAccP(acc) ^^ { _.immutable() }
  }

}

/** Некоторые парсеры завернуты в константы, чтобы дедублицировать инстансы парсеров. */
trait GidrometParsersVal extends GidrometParsers {
  override val floatNumber = super.floatNumber
  override val intNumberP = super.intNumberP

  override val nightP = super.nightP
  override val dayP = super.dayP

  override val withoutP = super.withoutP
  override val withP = super.withP
  override val proyasnP = super.proyasnP

  override val powerP = super.powerP
  override val smallP = super.smallP
  override val cloudyP = super.cloudyP
  override val rainP = super.rainP
  override val snowP = super.snowP
  override val percipationsP = super.percipationsP

  override val skyStateP = super.skyStateP
  override val precipation = super.precipation
  override val temperatures = super.temperatures
}
