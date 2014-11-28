package util.ai.sax.weather.gidromet

import models.ai._

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 18:32
 * Description: Утиль для поддержки работы с данными от гидрометцентра.
 */

/** Утиль для парсинга данных от гидромета. */
trait GidrometParsers extends JavaTokenParsers {

  def smallP: Parser[_] = "(?u)небольш\\w*".r
  def powerP: Parser[_] = "(?u)сильн\\w*".r

  def cloudyP: Parser[_] = "?(u)облачно\\w*".r
  def variableP: Parser[_] = "?(u)перемен\\w*".r
  def sunnyP: Parser[_] = "?(u)(солнечн|ясн)\\w*".r

  def withoutP: Parser[_] = "?(u)без".r
  def percipationsP: Parser[_] = "?(u)осадк\\w*".r
  def snowP: Parser[_] = "?(u)снег".r
  def rainP: Parser[_] = "?(u)дожд\\w*".r
  def rainShowerP: Parser[_] = "?(?u)ливе?н\\w*".r | (powerP ~> rainP)
  def thunderP: Parser[_] = "?(u)(гроз|молн|шторм)\\w*".r
  def iceP: Parser[_] = "?(u)ледян\\w*".r
  def fogP: Parser[_] = "(?u)туман\\w*".r


  def smallCloudy = (smallP <~ cloudyP) ^^^ SkyStates.CloudPartly
  def varCloudy = (variableP <~ cloudyP) ^^^ SkyStates.CloudVary
  def cloudy = cloudyP ^^^ SkyStates.Cloudy
  def sunny = sunnyP ^^^ SkyStates.Sunny

  def skyStateP: Parser[SkyState] = smallCloudy | varCloudy | cloudy | sunny


  def withoutPercipations = (withoutP ~> percipationsP) ^^^ Percipations.NoPercipations
  def smallRain = (smallP ~> rainP) ^^^ Percipations.SmallRain
  def rain = (rainP | rainShowerP) ^^^ Percipations.Rain
  def iceRain = (iceP ~> rain) ^^^ Percipations.IceRain
  def smallSnow = (smallP ~> snowP) ^^^ Percipations.SmallSnow
  def snow = snowP ^^^ Percipations.Snow
  def thunder = (opt(powerP) ~> thunderP) ^^^ Percipations.Thunder
  def fog = (opt(powerP) ~> fogP) ^^^ Percipations.Fog

  def percipation: Parser[Percipation] = {
    withoutPercipations | smallRain | iceRain | rain | smallSnow | snow | thunder | fog
  }

  def floatNumber = decimalNumber ^^ { _.toFloat }


  def temperatureP: Parser[_] = "(?u)темпер\\w*".r
  def dayP: Parser[_] = "(?u)(ден|дн[ёе]м)\\w*".r
  def nightP: Parser[_] = "(?u)ноч\\w*".r
  def celsiusP: Parser[_] = "°"

  def temperatures: Parser[Temperatures] = {
    val tempValP = floatNumber <~ celsiusP
    val temps = Temperatures()
    val nightTemp = (nightP ~> tempValP) ^^ { v => temps.nightOpt = Some(v) }
    val dayTemp = (dayP ~> tempValP) ^^ { v => temps.dayOpt = Some(v) }
    (temperatureP ~> opt((nightTemp ~ dayTemp) | dayTemp ~ nightTemp)) ^^^ temps
  }


  def windP: Parser[_] = "(?u)вете?р\\w*".r

  def eastP: Parser[_] = "(?u)вост\\w*".r
  def westP: Parser[_] = "(?u)запад\\w*".r
  def northP: Parser[_] = "(?u)север\\w*".r
  def southP: Parser[_] = "(?u)ю(жн|г)\\w*".r

  def geoDirectionP: Parser[WindDirection] = {
    val _eastP = eastP
    val _northP = northP
    val _westP = westP
    val _southP = southP
    val e = _eastP ^^^ WindDirections.EAST
    val w = _westP ^^^ WindDirections.WEST
    val n = _northP ^^^ WindDirections.NORTH
    val s = _southP ^^^ WindDirections.SOUTH
    val ne = (_northP ~ _eastP) ^^^ WindDirections.NORTH_EAST
    val nw = (_northP ~ _westP) ^^^ WindDirections.NORTH_WEST
    val se = (_southP ~ _eastP) ^^^ WindDirections.SOUTH_EAST
    val sw = (_southP ~ _westP) ^^^ WindDirections.SOUTH_WEST
    ne | nw | se | sw | e | w | n | s
  }

  def speedMs: Parser[Float] = {
    floatNumber <~ "[мm][/\\][cс]".r
  }

  // TODO Тут закончился рабочий день. Нужно допилить ветер, запилить атм.давление, вероятность осадков и тесты.
  def wind: Parser[_] = {
    windP ~> (geoDirectionP ~ speedMs) ^^ {
      case windDirection ~ windSpeed => ???
    }
  }

  def descrP(acc: DayWeatherBean): Parser[DayWeatherBean] = {
    val ssp = skyStateP ^^ {
      case ss  =>  acc.skyStateOpt = Some(ss)
    }
    val ppp = percipation ^^ {
      case pp =>  acc.percipations = pp :: acc.percipations
    }
    val tpp = temperatures ^^ {
      case ts =>  acc.temperatures = ts
    }
    rep(ssp | ppp | tpp) ^^^ acc
  }

}

trait GidrometParsersVal extends GidrometParsers {
  override val floatNumber = super.floatNumber

  override val powerP = super.powerP
  override val smallP = super.smallP
  override val cloudyP = super.cloudyP
  override val rainP = super.rainP
  override val snowP = super.snowP
  override val percipationsP = super.percipationsP

  override val skyStateP = super.skyStateP
  override val percipation = super.percipation
  override val temperatures = super.temperatures
}