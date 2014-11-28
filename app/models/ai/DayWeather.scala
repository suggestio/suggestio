package models.ai

import org.joda.time.LocalDate

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
case class DayWeatherBean(
  @BeanProperty var date              : LocalDate,
  @BeanProperty var skyStateOpt       : Option[SkyState] = None,
  @BeanProperty var percipations      : List[Percipation] = Nil,
  @BeanProperty var temperatures      : Temperatures = Temperatures(),
  @BeanProperty var pressureMmHgOpt   : Option[Int] = None,
  @BeanProperty var windDirectionOpt  : Option[WindDirection] = None,
  @BeanProperty var windSpeedMpsOpt   : Option[Float] = None,
  @BeanProperty var precipChanceOpt   : Option[Int] = None
) extends Serializable



/** Направления ветров. */
object WindDirections extends Enumeration {

  protected sealed class Val(name: String) extends super.Val(name) {
    def i18nCode: String = "wind.direction." + name
  }

  type WindDirection = Val

  val NONE        : WindDirection = new Val("none")
  val EAST        : WindDirection = new Val("east")
  val WEST        : WindDirection = new Val("west")
  val NORTH       : WindDirection = new Val("north")
  val SOUTH       : WindDirection = new Val("south")
  val NORTH_WEST  : WindDirection = new Val("north-west")
  val NORTH_EAST  : WindDirection = new Val("north-east")
  val SOUTH_WEST  : WindDirection = new Val("south-west")
  val SOUTH_EAST  : WindDirection = new Val("south-east")

  implicit def value2val(x: Value): WindDirection = x.asInstanceOf[WindDirection]

}

case class Temperatures(
  @BeanProperty var dayOpt: Option[Float] = None,
  @BeanProperty var nightOpt: Option[Float] = None
) {

  // Убогая java-хрень для взаимодейсвия со scalaSti без использования Option[].
  def getNightTemp: jl.Float = if (nightOpt.isDefined) nightOpt.get else null
  def setNightTemp(t: jl.Float): Unit = {
    nightOpt = Option(t)
  }

  def getDayTemp: jl.Float = if (dayOpt.isDefined) dayOpt.get else null
  def setDayTemp(t: jl.Float): Unit = {
    dayOpt = Option(t)
  }

}

/** Состояние неба: облачно, малооблачно, ясно и переменная облачность. */
object SkyStates extends Enumeration {
  type SkyState = Value
  val Sunny, CloudPartly, Cloudy, CloudVary = Value: SkyState
}

object Percipations extends Enumeration {
  type Percipation = Value
  val NoPercipations, Rain, SmallRain, SmallSnow, Snow, Thunder, IceRain, Fog = Value: Percipation
}
