package io.suggest.ym.model.common

import io.suggest.util.MacroLogsImplLazy
import io.suggest.ym.model.AdShowLevel
import io.suggest.ym.model.common.AdnSinks.AdnSink
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.14 17:29
 * Description: Из-за разделения уровней отображения по выдачам, появилась надстройка над отображаемыми типами,
 * которая содержит двух-буквенные коды, содержащие id выдачи (sink) и id уровня. Всё это токенизируется по ngram,
 * поэтому можно искать как по обоим флагам, так и по любому из них.
 */

object SinkShowLevels extends Enumeration with MacroLogsImplLazy {
  import LOGGER._
  
  protected def args2name(adnSink: AdnSink, sl: AdShowLevel): String = {
    adnSink.name + sl.name
  }

  /**
   * Одно значение enum'а.
   * @param adnSink sink
   * @param sl show level
   */
  protected case class Val(adnSink: AdnSink, sl: AdShowLevel)
    extends super.Val(args2name(adnSink, sl))
    with SlNameTokenStr
  {
    def name = adnSink.name + sl.name
  }

  type SinkShowLevel = Val

  implicit def value2val(v: Value): SinkShowLevel = v.asInstanceOf[SinkShowLevel]

  // Гео-уровни отображения.
  val GEO_START_PAGE_SL: SinkShowLevel = Val(AdnSinks.SINK_GEO, AdShowLevels.LVL_START_PAGE)
  val GEO_CATS_SL: SinkShowLevel = Val(AdnSinks.SINK_GEO, AdShowLevels.LVL_CATS)
  val GEO_PRODUCER_SL: SinkShowLevel = Val(AdnSinks.SINK_GEO, AdShowLevels.LVL_PRODUCER)

  // wifi-уровни отображения.
  val WIFI_START_PAGE_SL: SinkShowLevel = Val(AdnSinks.SINK_WIFI, AdShowLevels.LVL_START_PAGE)
  val WIFI_CATS_SL: SinkShowLevel = Val(AdnSinks.SINK_WIFI, AdShowLevels.LVL_CATS)
  val WIFI_PRODUCER_SL: SinkShowLevel = Val(AdnSinks.SINK_WIFI, AdShowLevels.LVL_PRODUCER)


  def maybeWithName(n: String): Option[SinkShowLevel] = {
    try {
      Some(withName(n))
    } catch {
      case ex: Exception => None
    }
  }


  /**
   * Найти элемент enum с указанными полями.
   * @param adnSink Используемый sink.
   * @param sl Уровень отображения в рамках sink.
   * @return SinkShowLevel.
   */
  def withArgs(adnSink: AdnSink, sl: AdShowLevel): SinkShowLevel = {
    withName(args2name(adnSink, sl))
  }

  /** Поиск с учетом совместимости с slsPub/slsWant, когда всё было wifi-only. */
  def fromAdSl(sl: AdShowLevel): SinkShowLevel = {
    withName(args2name(AdnSinks.SINK_WIFI, sl))
  }
  

  /** Все уровни отображения для wifi-sink'ов. */
  def wifiSls = List(WIFI_START_PAGE_SL, WIFI_CATS_SL, WIFI_PRODUCER_SL)

  /** Все уровни отображения для geo-синков. */
  def geoSls = List(GEO_START_PAGE_SL, GEO_CATS_SL, GEO_PRODUCER_SL)


  /** Десериализатор значений из самых примитивных типов и коллекций. */
  val deserializeLevelsSet: PartialFunction[Any, Set[SinkShowLevel]] = {
    case v: java.lang.Iterable[_] =>
      v.foldLeft[List[SinkShowLevel]] (Nil) { (acc, slRaw) =>
        maybeWithName(slRaw.toString) match {
          case Some(sl) =>
            sl :: acc
          case None =>
            warn(s"Unable to deserialize show level '$slRaw'. Possible levels are: ${values.mkString(", ")}")
            acc
        }
      }.toSet
  }

  /** compat-десериализация на основе уровней slsPub. */
  val deserializeFromAdSls: PartialFunction[Any, Set[SinkShowLevel]] = {
    AdShowLevels.deserializeShowLevels andThen { sls =>
      sls.map { fromAdSl }
    }
  }

  implicit def sls2strings(sls: Set[SinkShowLevel]) = sls.map(_.name)
}


/** Поле name для поиска по полю sls 2014-aug, которое содержит ngram'мы различной длины.
  * Можно искать по sink-name из [[AdnSinks]], по [[AdShowLevels]] и по полному имени [[SinkShowLevels]]. */
trait SlNameTokenStr {
  def name: String
}

