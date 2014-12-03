package models.ai

import org.joda.time.LocalDate
import util.TplDataFormatUtil

import scala.beans.BeanProperty
import java.{lang => jl}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 17:23
 * Description: Модель для представления погоды за один день.
 * Т.к. эти данные будут переданы в scalaSti, то надо завернуть их согласно JavaBeans.
 * Изменчивость состояния также позволяет использовать это в качестве аккамулятора в поточных обработчиках типа
 * SAX-парсеров.
 */
case class DayWeatherAcc(
  var date              : LocalDate,
  var skyStateOpt       : Option[SkyState] = None,
  var precipations      : List[Precipation] = Nil,
  var temperatures      : Temperatures = Temperatures(),
  var pressureMmHg      : AtmPressure = AtmPressure(),
  var windOpt           : Option[Wind] = None,
  var precipChanceOpt   : Option[Int] = None
) {

  def immutable() = {
    DayWeatherBean(
      date            = date,
      skyStateOpt     = skyStateOpt,
      precipations    = precipations,
      temperatures    = temperatures,
      pressureMmHg    = pressureMmHg,
      windOpt         = windOpt,
      precipChanceOpt = precipChanceOpt
    )
  }

}


/** immutable bean версия аккамулятора погоды за один день. */
case class DayWeatherBean(
  @BeanProperty date              : LocalDate,
  skyStateOpt                     : Option[SkyState],
  @BeanProperty precipations      : List[Precipation],
  @BeanProperty temperatures      : Temperatures,
  @BeanProperty pressureMmHg      : AtmPressure,
  @BeanProperty windOpt           : Option[Wind],
  @BeanProperty precipChanceOpt   : Option[Int]
) extends ContentHandlerResult


/** Интерфейс готового прогноза погоды: сегодня, завтра и послезавтра если есть. */
trait WeatherForecastT extends ContentHandlerResult {
  /** Погода на сегодня. */
  def getToday: DayWeatherBean
  /** Погода на завтра, если есть. */
  def getTomorrow: Option[DayWeatherBean]
  /** Погода на послезавтра, если есть. */
  def getAfterTomorrow: Option[DayWeatherBean]
}


/** Направления ветров. */
object GeoDirections extends Enumeration {

  protected sealed class Val(name: String) extends super.Val(name) {
    def i18nCode: String = "wind.direction." + name
  }

  type GeoDirection = Val

  val NONE        : GeoDirection = new Val("none")
  val EAST        : GeoDirection = new Val("east")
  val WEST        : GeoDirection = new Val("west")
  val NORTH       : GeoDirection = new Val("north")
  val SOUTH       : GeoDirection = new Val("south")
  val NORTH_WEST  : GeoDirection = new Val("north-west")
  val NORTH_EAST  : GeoDirection = new Val("north-east")
  val SOUTH_WEST  : GeoDirection = new Val("south-west")
  val SOUTH_EAST  : GeoDirection = new Val("south-east")

  implicit def value2val(x: Value): GeoDirection = x.asInstanceOf[GeoDirection]

}


/** Температуры. */
case class Temperatures(
  @BeanProperty var dayOpt: Option[Float] = None,
  @BeanProperty var nightOpt: Option[Float] = None
) {

  protected def formatTemp(t: Option[Float]): String = {
    if (t.isDefined)  TplDataFormatUtil.formatTemperature(t.get)  else  ""
  }

  // JavaBean-интерфейс для взаимодейсвия со scalaSti рендерером без использования Option[].
  def getNight = formatTemp(nightOpt)
  def getDay = formatTemp(dayOpt)

}

/** Состояние неба: ясно, малооблачно, облачно и переменная облачность. */
object SkyStates extends Enumeration {

  sealed protected class Val(@BeanProperty val name: String) extends super.Val(name) {
    def getL10nId = "weather.sky.state." + name
  }

  type SkyState = Val

  val Clear         : SkyState = new Val("Clear")
  val CloudPartly   : SkyState = new Val("PartlyCloudy")
  val Cloudy        : SkyState = new Val("Cloudy")
  val CloudVary     : SkyState = new Val("VaryCloudy")
}

object Precipations extends Enumeration {
  type Precipation = Value
  val NoPercipations, Rain, SmallRain, SmallSnow, Snow, Thunder, IceRain, Fog = Value: Precipation
}


/**
 * Характеристики ветра.
 * @param direction Направление ветра.
 * @param speedMps Скорость ветра.
 */
case class Wind(direction: WindDirection, speedMps: Float)


/**
 * Описание атмосферного давления днём и ночью.
 * @param dayOpt Давление днём.
 * @param nightOpt Давление ночью.
 */
case class AtmPressure(
  var dayOpt: Option[Int] = None,
  var nightOpt: Option[Int] = None
) {

  private def formatPressure(p: Option[Int], default: Option[Int]): String = {
    if (p.isDefined)  {
      p.get.toString
    } else if (default.isDefined) {
      default.get.toString
    } else {
      ""
    }
  }

  def getDay = formatPressure(dayOpt, nightOpt)
  def getNight = formatPressure(nightOpt, dayOpt)

}

