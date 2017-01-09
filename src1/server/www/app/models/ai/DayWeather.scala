package models.ai

import org.joda.time.{DateTime, LocalDate}
import play.api.i18n.Messages
import util.TplDataFormatUtil

import scala.annotation.meta.getter
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
  precipations                    : List[Precipation],
  @BeanProperty temperatures      : Temperatures,
  @BeanProperty pressureMmHg      : AtmPressure,
  windOpt                         : Option[Wind],
  precipChanceOpt                 : Option[Int]
) extends ContentHandlerResult {

  val getWind = windOpt getOrElse Wind(GeoDirections.NONE, 0F)
  def getPrecipChance: String = if (precipChanceOpt.isDefined)  precipChanceOpt.get.toString  else  "?"
}


/** Интерфейс готового прогноза погоды: сегодня, завтра и послезавтра если есть. */
trait WeatherForecastT extends ContentHandlerResult {
  /** Погода на сегодня. */
  def getToday: DayWeatherBean
  /** Погода на завтра, если есть. */
  def getTomorrow: Option[DayWeatherBean]
  /** Погода на послезавтра, если есть. */
  def getAfterTomorrow: Option[DayWeatherBean]

  /** Локальное время в точке, к которой относится этот прогноз. */
  def getLocalTime: DateTime = DateTime.now

  /** В зависимости от текущего времени нужно сгенерить прогноз погоды на день и ночь или наоборот. */
  def getH24: DayWeatherBean = {
    val hod = getLocalTime.getHourOfDay
    if (hod > 18 || hod < 5) {
      // Если ночь, то надо отобразить сегодняшнюю ночь и завтрашний день
      val today = getToday
      val tmrOpt = getTomorrow
      today.copy(
        temperatures = today.temperatures.copy(
          dayOpt = tmrOpt.flatMap(_.temperatures.dayOpt).orElse(today.temperatures.dayOpt)
        ),
        pressureMmHg = today.pressureMmHg.copy(
          dayOpt = tmrOpt.flatMap(_.pressureMmHg.dayOpt).orElse(today.pressureMmHg.dayOpt)
        )
      )
    } else {
      // Если не ночь, то отобразить погоду на сейчас
      getToday
    }
  }
}


/** Направления ветров. */
object GeoDirections extends Enumeration {

  sealed protected trait ValT {
    val getName: String
    def i18nCode: String = "wind.direction." + getName
    def getVDirection: GeoDirection
  }

  protected sealed class Val(@BeanProperty val getName: String) extends super.Val(getName) with ValT {
    @BeanProperty
    override def getVDirection: GeoDirection = this
  }

  trait NorthV extends ValT {
    @BeanProperty override def getVDirection = NORTH
  }
  trait SouthV extends ValT {
    @BeanProperty override def getVDirection = SOUTH
  }

  type GeoDirection = Val

  val NONE        : GeoDirection = new Val("none") {
    override def i18nCode: String = ""
  }
  val EAST        : GeoDirection = new Val("east")
  val WEST        : GeoDirection = new Val("west")
  val NORTH       : GeoDirection = new Val("north")
  val SOUTH       : GeoDirection = new Val("south")
  val NORTH_WEST  : GeoDirection = new Val("north-west") with NorthV
  val NORTH_EAST  : GeoDirection = new Val("north-east") with NorthV
  val SOUTH_WEST  : GeoDirection = new Val("south-west") with SouthV
  val SOUTH_EAST  : GeoDirection = new Val("south-east") with SouthV

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
case class Wind(@BeanProperty direction: GeoDirection, @BeanProperty speedMps: Float)


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

