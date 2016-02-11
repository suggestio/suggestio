package io.suggest.model.sc.common

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT
import io.suggest.util.MacroLogsImplLazy
import AdShowLevels._
import io.suggest.ym.model.common.AdnSinks._
import io.suggest.ym.model.common._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.14 17:29
 * Description: Из-за разделения уровней отображения по выдачам, появилась надстройка над отображаемыми типами,
 * которая содержит двух-буквенные коды, содержащие id выдачи (sink) и id уровня. Всё это токенизируется по ngram,
 * поэтому можно искать как по обоим флагам, так и по любому из них.
 */

object SinkShowLevels extends EnumMaybeWithName with MacroLogsImplLazy with EnumJsonReadsValT {

  import LOGGER._

  protected def args2name(adnSink: AdnSink, sl: AdShowLevel): String = {
    adnSink.name + sl.name
  }

  /**
   * Одно значение enum'а.
   */
  protected abstract class Val(val name: String)
    extends super.Val(name)
    with SlNameTokenStr
  {
    /** adnSink sink */
    def adnSink: AdnSink
    /** show level */
    def sl: AdShowLevel
  }

  override type T = Val


  // Гео-уровни отображения.
  private trait _Geo extends Val {
    override def adnSink  = SINK_GEO
  }
  val GEO_START_PAGE_SL   : T = new Val( args2name(SINK_GEO, LVL_START_PAGE) ) with _Geo {
    override def sl = LVL_START_PAGE
  }
  @deprecated("Categories support is removed", "2015.nov")
  val GEO_CATS_SL         : T = new Val( args2name(SINK_GEO, LVL_CATS) ) with _Geo {
    override def sl = LVL_CATS
  }
  val GEO_PRODUCER_SL     : T = new Val( args2name(SINK_GEO, LVL_PRODUCER) ) with _Geo {
    override def sl = LVL_PRODUCER
  }


  // wifi-уровни отображения.
  @deprecated("wifi routers support is removed", "2016")
  private trait _Wifi extends Val {
    override def adnSink = SINK_WIFI
  }
  @deprecated("wifi routers support is removed", "2016")
  val WIFI_START_PAGE_SL  : T = new Val( args2name(SINK_WIFI, LVL_START_PAGE) ) with _Wifi {
    override def sl = LVL_START_PAGE
  }
  @deprecated("wifi routers support is removed", "2016")
  val WIFI_CATS_SL        : T = new Val( args2name(SINK_WIFI, LVL_CATS) ) with _Wifi {
    override def sl = LVL_CATS
  }
  @deprecated("wifi routers support is removed", "2016")
  val WIFI_PRODUCER_SL    : T = new Val( args2name(SINK_WIFI, LVL_PRODUCER) ) with _Wifi {
    override def sl = LVL_PRODUCER
  }


  /**
   * Найти элемент enum с указанными полями.
   * @param adnSink Используемый sink.
   * @param sl Уровень отображения в рамках sink.
   * @return SinkShowLevel.
   */
  def withArgs(adnSink: AdnSink, sl: AdShowLevel): T = {
    withName(args2name(adnSink, sl))
  }

  /** Поиск с учетом совместимости с slsPub/slsWant, когда всё было wifi-only. */
  def fromAdSl(sl: AdShowLevel): T = {
    withName(args2name(AdnSinks.SINK_WIFI, sl))
  }


  /** Все уровни отображения для wifi-sink'ов. */
  def wifiSls = List(WIFI_START_PAGE_SL, WIFI_CATS_SL, WIFI_PRODUCER_SL)

  /** Все уровни отображения для geo-синков. */
  def geoSls = List(GEO_START_PAGE_SL, GEO_CATS_SL, GEO_PRODUCER_SL)


  /** Десериализатор значений из самых примитивных типов и коллекций. */
  val deserializeLevelsSet: PartialFunction[Any, Set[T]] = {
    case v: java.lang.Iterable[_] =>
      import scala.collection.JavaConversions._
      v.foldLeft[List[T]] (Nil) { (acc, slRaw) =>
        maybeWithName(slRaw.toString) match {
          case Some(sl) =>
            sl :: acc
          case None =>
            warn(s"Unable to deserialize show level '$slRaw'. Possible levels are: ${values.mkString(", ")}")
            acc
        }
      }.toSet
  }

  def sls2strings(sls: Set[T]) = sls.map(_.name)

  /** Десериализация уровней отображения. */
  def applySlsSet(slsRaw: TraversableOnce[String]): Set[SinkShowLevel] = {
    slsRaw
      .toIterator
      .map { slRaw =>
        val result = if (slRaw.length == 1) {
          // compat: парсим slsPub, попутно конвертя их в sink-версии
          val sl = AdShowLevels.withName(slRaw)
          SinkShowLevels.fromAdSl(sl)
        } else {
          SinkShowLevels.withName(slRaw)
        }
        result : SinkShowLevel
      }
      .toSet
  }

  /** Сериализация уровней отображения. */
  def unapplySlsSet(sls: Set[SinkShowLevel]): Option[Seq[String]] = {
    val seq = sls.iterator
      .map(_.name)
      .toSeq
    Some(seq)
  }

}
