package io.suggest.ym.model

import io.suggest.common.menum.{EnumJsonReadsT, EnumMaybeWithId, EnumMaybeWithName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 18:38
 * Description: Гео-уровни, т.е. отражают используемые поля и влияют на их индексацию.
 */
object NodeGeoLevels extends Enumeration(1) with EnumMaybeWithName with EnumMaybeWithId with EnumJsonReadsT {

  /** Класс экземпляров модели. */
  protected[this] abstract class Val(val esfn: String)
    extends super.Val(esfn)
  {

    /** Заявленная точность. */
    def precision: String

    // Для упрощения геморроя делаем локализацию прямо тут, хотя сами коды лежат на стороне sioweb21/conf/messages.*
    def l10nSingular = "ngl." + esfn
    def l10nSingularShort = l10nSingular

    def l10nPlural = "ngls." + esfn
    def l10nPluralShort = l10nPlural

    /** Это самый нижний слой с самым мелким масштабом? */
    def isLowest: Boolean = false

    /** Это самый верхний слой с самым крупным масштабом? */
    def isHighest: Boolean = false

    /** Это крайний слой? */
    def isOutermost = isLowest || isHighest

    /** Рекурсивный метод для пошагового накопления уровней и аккамулятор.
      * @param currLevel текущий (начальный) уровень.
      * @param acc Аккамулятор.
      * @param nextLevelF Функция перехода на следующий уровень с текущего.
      * @return Аккамулятор накопленных уровней.
      */
    private def collectLevels(currLevel: T = this, acc: List[T] = Nil)
                             (nextLevelF: T => Option[T]): List[T] = {
      val nextOpt = nextLevelF(currLevel)
      if (nextOpt.isDefined) {
        val _next = nextOpt.get
        collectLevels(_next, _next :: acc)(nextLevelF)
      } else {
        acc
      }
    }

    def lower: Option[T]
    def lowerOfThis: T = lower getOrElse this
    def allLowerLevels: List[T] = collectLevels()(_.lower)

    def upper: Option[T]
    def upperOrThis: T = upper getOrElse this
    def allUpperLevels: List[T] = collectLevels()(_.upper)

    /** Предлагаемый масштаб osm-карты при отображении объекта на ней в полный рост.
      * Это НЕ МЕТРЫ, а порядковый номер по какой-то ихней шкале масштабов. */
    def osmMapScale: Int

    /** Точность в метрах, в рамках которой имеет смысл детектить что-либо на этом уровне. */
    def accuracyMetersMax: Option[Int] = None //CONFIG.getInt(s"geo.node.level.$esfn.accuracy.max.meters")
  }

  override type T = Val

  // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  // !!! Двигаемся от мелкому к крупному. На этом принципе построены зависмые модели, например web21/GeoMode. !!!
  // !!!            Нарушение порядка приведет к трудноотлавливаемым логическим ошибкам.                      !!!
  // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

  val NGL_BUILDING: T = new Val("bu") {
    override def isLowest = true
    override def lower: Option[T] = None
    override def upper: Option[T] = Some(NGL_TOWN_DISTRICT)
    override def precision = "50m"
    override def osmMapScale = 16
    override val accuracyMetersMax: Option[Int] = {
      super.accuracyMetersMax
        .orElse { Some(400) }
    }
  }

  val NGL_TOWN_DISTRICT: T = new Val("td") {
    override def lower: Option[T] = Some(NGL_BUILDING)
    override def upper: Option[T] = Some(NGL_TOWN)
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

  val NGL_TOWN: T = new Val("to") {
    override def lower: Option[T] = Some(NGL_TOWN_DISTRICT)
    override def upper: Option[T] = None
    override def precision = "5km"
    override def isHighest = true
    override def osmMapScale = 10
    override def accuracyMetersMax: Option[Int] = None
  }

  def default = NGL_BUILDING


  /** Layer, относящийся к гео-тегам. */
  def geoTag = geoPlace

  /** Layer, в котором карточки размещают на карте. */
  def geoPlace = NGL_TOWN_DISTRICT

  /** Вывести множество значений этого enum'а, но выставив текущий тип значения вместо Value. */
  def valuesNgl: Set[T] = values.asInstanceOf[Set[T]]

}

