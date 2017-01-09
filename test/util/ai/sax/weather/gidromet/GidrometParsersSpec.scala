package util.ai.sax.weather.gidromet

import functional.RegexParsersTesting
import models.ai._
import org.apache.lucene.store.WindowsDirectory
import org.joda.time.LocalDate
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 11:09
 * Description: Тесты для парсеров данных гидрометцентра.
 */
class GidrometParsersSpec extends PlaySpec with GidrometParsersVal with RegexParsersTesting {


  "SkyState sub-parsers" must {

    import SkyStates._

    "see clear sky" in {
      def t(s: String) = parseSuccess(clearSky, s) mustBe Clear
      t("ясно")
      t("ясн")
      t("солнечно")
      t("солнеч")
    }

    "see cloudy sky" in {
      def t(s: String) = parseSuccess(cloudy, s) mustBe Cloudy
      t("облачно")
      t("облачн")
      t("облач")
      t("облачн без прояснен")
      t("облачно без проясненения")
    }

    "see partly cloudy sky" in {
      def t(s: String) = parseSuccess(partlyCloudy, s) mustBe CloudPartly
      t("малооблачно")
      t("мал облачн")
      t("небольшая облачность")
      t("небольш облачн")
      t("слабая облачность")
    }

    "see variable cloudy sky" in {
      def t(s: String) = parseSuccess(varCloudy, s) mustBe CloudVary
      t("переменная облачность")
      t("перемен облачн")
      t("облачно с прояснениями")
      t("облач с прояснениям")
    }

    "overally see different sky states" in {
      def t(s: String) = parseSuccess(skyStateP, s)
      t("облачн")                     mustBe Cloudy
      t("облачно с прояснениями")     mustBe CloudVary
      t("облач с проясн")             mustBe CloudVary
      t("ясн")                        mustBe Clear
      t("солнечно")                   mustBe Clear
      t("небольшая облачность")       mustBe CloudPartly
      t("небольш облачн")             mustBe CloudPartly
    }

  }


  "Precipation parsers" must {

    import Precipations._

    "see non-precipative weather" in {
      def t(s: String) = parseSuccess(withoutPercipations, s) mustBe NoPercipations
      t("без осадков")
      t("без осадк")
    }

    "see small rain" in {
      def t(s: String) = parseSuccess(smallRain, s) mustBe SmallRain
      t("небольшой дождь")
      t("небольш дожд")
      t("слабый дождь")
      t("слаб дожд")
    }

    "see good rain" in {
      def t(s: String) = parseSuccess(rain, s) mustBe Rain
      t("дождь")
      t("дожд")
      t("ливень")
      t("ливен")
      t("сильный дождь")
    }

    "see ice rain hell" in {
      def t(s: String) = parseSuccess(iceRain, s) mustBe IceRain
      t("ледяной дождь")
      t("ледян дожд")
    }

    "see thunder" in {
      def t(s: String) = parseSuccess(thunder, s) mustBe Thunder
      t("гроза")
      t("сильная гроза")
      t("буря")
      t("сильная буря")
      t("грозовая буря")
      t("молния")
      t("дождь с грозой")
      t("дождь с громом и молнией")
      t("гром и молния")
      t("грозовая буря")
    }

    "see fog" in {
      def t(s: String) = parseSuccess(fog, s) mustBe Fog
      t("туман")
      t("сильный туман")
      t("сильн туман")
      t("небольшой туман")
      t("небольш туман")
    }

    "see snow" in {
      def t(s: String) = parseSuccess(snow, s) mustBe Snow
      t("снег")
      t("снегопад")
      t("сильный снегопад")
    }

    "see small snow" in {
      def t(s: String) = parseSuccess(smallSnow, s) mustBe SmallSnow
      t("небольшой снег")
      t("слабый снег")
      t("небольш снегопад")
    }


    "overally see different precipations" in {
      def t(s: String) = parseSuccess(precipation, s)
      t("небольшой снег")     mustBe SmallSnow
      t("небольшой дождь")    mustBe SmallRain
      t("без осадков")        mustBe NoPercipations
      t("дождь")              mustBe Rain
      t("сильный дождь")      mustBe Rain
      t("слабый дождь")       mustBe SmallRain
      t("снегопад")           mustBe Snow
      t("дождь с грозой")     mustBe Thunder
      t("гроза")              mustBe Thunder
      t("туманно")            mustBe Fog
    }

  }


  "Temperature parsers" must {
    "see day and night temperatures" in {
      def t(s: String) = parseSuccess(temperatures, s)
      t("температура ночью -4° днём -2°")   mustBe Temperatures(dayOpt = Some(-2F),  nightOpt = Some(-4F))
      t("температура днем -2° ночью -4°")   mustBe Temperatures(dayOpt = Some(-2F),  nightOpt = Some(-4F))
      t("температура днем +2° ночью +4°")   mustBe Temperatures(dayOpt = Some(2F),   nightOpt = Some(4F))
      t("температура днем +2 ночью 4")      mustBe Temperatures(dayOpt = Some(2F),   nightOpt = Some(4F))
      t("температура днем +22° ночью 0°")   mustBe Temperatures(dayOpt = Some(22F),  nightOpt = Some(0F))
    }
  }


  "Wind parsers" must {
    import GeoDirections._

    "see different geo directions" in {
      def t(s: String) = parseSuccess(geoDirectionP, s)
      t("северо западный")  mustBe NORTH_WEST
      t("северно восток")   mustBe NORTH_EAST
      t("север")            mustBe NORTH
      t("северн")           mustBe NORTH
      t("юго восточный")    mustBe SOUTH_EAST
      t("южно западный")    mustBe SOUTH_WEST
      t("южный")            mustBe SOUTH
    }

    "see speed in m/s" in {
      def t(s: String, v: Float) = parseSuccess(speedMs, s) mustBe v
      t("2 м/с",    2F)
      t("333 м/c",  333F)
      t("0 м/c",    0F)
    }

    "overally see wind characteristics" in {
      def t(s: String) = parseSuccess(windP, s)
      t("ветер юго западный 4 м/с")       mustBe Wind(SOUTH_WEST, 4F)
      t("ветер северный 666 м/c")         mustBe Wind(NORTH, 666F)
      t("ветер юго-восточный 10 м/с")     mustBe Wind(SOUTH_EAST, 10)
    }

  }


  "Atm.pressure parsers" must {
    "see daily barometric pressures" in {
      def t(s: String) = parseSuccess(pressureP(AtmPressure()), s)
      t("атмосферное давление ночью 773 мм рт.ст. днём 772 мм рт.ст")   mustBe AtmPressure(Some(772), Some(773))
      t("атмосферное давление днем 772 мм рт.ст. ночью 773 мм рт.ст")   mustBe AtmPressure(Some(772), Some(773))
      t("атмосферн давлен дне 772 мм рт ст ночь 773 мм рт ст")          mustBe AtmPressure(Some(772), Some(773))
    }
  }


  "Precipations chance parsers" must {
    "see precip. chance" in {
      def t(s: String, c: Int) = parseSuccess(precipChanceP, s) mustBe c
      t("вероятность осадков 21%",  21)
      t("вероятн осадк 21%",        21)
      t("вероятн осадк 0%",         0)
    }
  }


  "Day weather parser" must {
    "see weather in lucene-normalized gidromet rss description" in {
      val today = LocalDate.now()
      def t(s: String) = parseSuccess(dayWeatherP(DayWeatherAcc(today)), s)
      t("переменная облачность без осадков температура ночью -4° днём -2° ветер юго-западный 4 м/с атмосферное давление ночью 772 мм рт.ст. днём 770 мм рт.ст. вероятность осадков 21%"
      ) mustBe DayWeatherBean(
        date            = today,
        skyStateOpt     = Some(SkyStates.CloudVary),
        precipations    = List(Precipations.NoPercipations),
        temperatures    = Temperatures(Some(-2), nightOpt = Some(-4)),
        pressureMmHg    = AtmPressure(Some(770), nightOpt = Some(772)),
        windOpt         = Some(Wind(GeoDirections.SOUTH_WEST, 4)),
        precipChanceOpt = Some(21)
      )
    }
  }

}

