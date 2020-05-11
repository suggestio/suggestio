package io.suggest.geo

import enumeratum._
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq._
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 18:38
 * Description: Гео-уровни, т.е. отражают используемые поля и влияют на их индексацию.
 */

/** Модель гео-уровней для индексации гео-данных в различном масштабе. */
object MNodeGeoLevels extends Enum[MNodeGeoLevel] {

  // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // !!! Двигаемся от мелкому к крупному. На этом принципе построены зависмые модели, например web21/GeoMode. !!!
  // !!!            Нарушение порядка приведет к трудноотлавливаемым логическим ошибкам.                      !!!
  // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

  case object NGL_BUILDING extends MNodeGeoLevel {
    override def id = 1
    override def esfn = "bu"
    override def isLowest = true
    override def lower: Option[MNodeGeoLevel] = {
      None
    }
    override def upper: Option[MNodeGeoLevel] = {
      Some(NGL_TOWN_DISTRICT)
    }
    override def precision = "50m"
    override def osmMapScale = 16
    override val accuracyMetersMax: Option[Int] = {
      super.accuracyMetersMax
        .orElse { Some(400) }
    }
  }

  case object NGL_TOWN_DISTRICT extends MNodeGeoLevel {
    override def id = 2
    override def esfn = "td"
    override def lower: Option[MNodeGeoLevel] = {
      Some(NGL_BUILDING)
    }
    override def upper: Option[MNodeGeoLevel] = {
      Some(NGL_TOWN)
    }
    override def precision = "800m"
    override def osmMapScale = 12

    /** У "Районов города" есть короткое название - "Районы" */
    override val l10nSingularShort = "District"
    override val l10nPluralShort = l10nSingularShort + "s"
    override val accuracyMetersMax: Option[Int] = {
      super.accuracyMetersMax
        .orElse { Some(3000) }
    }
  }

  case object NGL_TOWN extends MNodeGeoLevel {
    override def id = 3
    override def esfn = "to"
    override def lower: Option[MNodeGeoLevel] = {
      Some(NGL_TOWN_DISTRICT)
    }
    override def upper: Option[MNodeGeoLevel] = {
      None
    }
    override def precision = "5km"
    override def isHighest = true
    override def osmMapScale = 10
    override def accuracyMetersMax: Option[Int] = None
  }

  final def default: MNodeGeoLevel = NGL_BUILDING


  /** Layer, относящийся к гео-тегам. */
  final def geoTag: MNodeGeoLevel = NGL_TOWN_DISTRICT

  /** Layer, в котором карточки размещают на карте. */
  final def geoPlace: MNodeGeoLevel = NGL_BUILDING

  /** В каких гео-слоях искать geo-place-размещения?
    *
    * 2018-03-23 Изначально, размещения в geo-place было в уровне TOWN_DISTRICT.
    * Но этого оказалось мало. Поэтому, теперь они на уровне здания, а тут поддержка
    * для поиска карточек в обоих индексах сразу.
    */
  final def geoPlacesSearchAt = geoPlace :: Nil


  override val values = findValues


  def withIdOption(id: Int): Option[MNodeGeoLevel] = {
    values.find(_.id ==* id)
  }

}


/** Класс одного элемента модели гео-уровней. */
sealed abstract class MNodeGeoLevel extends EnumEntry {

  def esfn: String

  override final def entryName = esfn

  /** Порядковый номер слоя. Используется в umap-редакторе, т.к. та не понимает строковых id. */
  def id: Int

  /** Заявленная точность. */
  def precision: String

  // Для упрощения геморроя делаем локализацию прямо тут, хотя сами коды лежат на стороне sioweb21/conf/messages.*
  def l10nSingular: String = {
    "ngl." + esfn
  }

  def l10nSingularShort: String = l10nSingular

  def l10nPlural: String = {
    "ngls." + esfn
  }
  def l10nPluralShort: String = l10nPlural

  /** Это самый нижний слой с самым мелким масштабом? */
  def isLowest: Boolean = false

  /** Это самый верхний слой с самым крупным масштабом? */
  def isHighest: Boolean = false

  /** Это крайний слой? */
  def isOutermost: Boolean = {
    isLowest || isHighest
  }

  /** Рекурсивный метод для пошагового накопления уровней и аккамулятор.
    * @param currLevel текущий (начальный) уровень.
    * @param acc Аккамулятор.
    * @param nextLevelF Функция перехода на следующий уровень с текущего.
    * @return Аккамулятор накопленных уровней.
    */
  private def collectLevels(currLevel: MNodeGeoLevel = this, acc: List[MNodeGeoLevel] = Nil)
                           (nextLevelF: MNodeGeoLevel => Option[MNodeGeoLevel]): List[MNodeGeoLevel] = {
    val nextOpt = nextLevelF(currLevel)
    if (nextOpt.isDefined) {
      val _next = nextOpt.get
      collectLevels(_next, _next :: acc)(nextLevelF)
    } else {
      acc
    }
  }

  def lower: Option[MNodeGeoLevel]
  def lowerOfThis: MNodeGeoLevel = lower getOrElse this
  def allLowerLevels: List[MNodeGeoLevel] = collectLevels()(_.lower)

  def upper: Option[MNodeGeoLevel]
  def upperOrThis: MNodeGeoLevel = upper getOrElse this
  def allUpperLevels: List[MNodeGeoLevel] = collectLevels()(_.upper)

  /** Предлагаемый масштаб osm-карты при отображении объекта на ней в полный рост.
    * Это НЕ МЕТРЫ, а порядковый номер по какой-то ихней шкале масштабов. */
  def osmMapScale: Int

  /** Точность в метрах, в рамках которой имеет смысл детектить что-либо на этом уровне. */
  def accuracyMetersMax: Option[Int] = None //CONFIG.getInt(s"geo.node.level.$esfn.accuracy.max.meters")

}


object MNodeGeoLevel {

  implicit def NODE_GEO_LEVEL_FORMAT: Format[MNodeGeoLevel] = {
    EnumeratumUtil.enumEntryFormat( MNodeGeoLevels )
  }

  @inline implicit def univEq: UnivEq[MNodeGeoLevel] = UnivEq.derive

}
