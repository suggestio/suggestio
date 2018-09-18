package io.suggest.geo

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.04.16 17:47
  * Description: Известные источники геолокаций, с которыми взаимодействует
  * система client-side.
  *
  * В браузерах исторически есть точная и неточная геолокации.
  */

object GeoLocTypes extends StringEnum[GeoLocType] {

  /** Неточная быстрая геолокация по вышкам. */
  case object Bss extends GeoLocType("b") {
    override def precision        = 20
    override def highAccuracy     = false
    override def previous         = None
    override def suppressorTtlMs  = None
  }

  /** Точная медленная геолокация по спутникам. */
  case object Gps extends GeoLocType("g") {
    override def precision        = 50
    override def highAccuracy     = true
    override def previous         = Some(Bss)
    /** Считаем для GPS необходимость отклика в течение 10 секунд максимум. */
    override def suppressorTtlMs  = Some(10000)
  }


  override val values = findValues

  /** Все значения модели. */
  def all = Set[GeoLocType](Gps, Bss)
  /** Минимальная точность у вышек. */
  def min = Bss
  /** Максимальная точность у gps. */
  def max = Gps

}


/** Класс одного элемента модели [[GeoLocTypes]]. */
sealed abstract class GeoLocType(override val value: String) extends StringEnumEntry {

  /** Значение для HTML5 GeoLocation API highAccuracy. */
  def highAccuracy: Boolean

  /** Порядок точности и сортировки одновременно. У GPS наибольшая географическая точность. */
  def precision: Int

  /** Предшествующий по точности элемент. */
  def previous: Option[GeoLocType]

  /** Все предшествующие по точности элементы. */
  def allPrevious: List[GeoLocType] = {
    previous.fold [List[GeoLocType]] (Nil) { p =>
      p :: p.allPrevious
    }
  }

  /**
    * Если данному типу разрешено доминировать над нижележащими, то тут указыватеся макс.время
    * доминирования в миллисекундах. Если за это время от источника не поступило свежих новостей, то
    * подавление нижележащих источников отменяется.
    */
  def suppressorTtlMs: Option[Int]

}


object GeoLocType {

  /** Поддержка сортировки с учетом ордеров. */
  implicit def ordering: Ordering[GeoLocType] = new Ordering[GeoLocType] {
    override def compare(x: GeoLocType, y: GeoLocType): Int = {
      x.precision - y.precision
    }
  }

  @inline implicit def univEq: UnivEq[GeoLocType] = UnivEq.derive

}


