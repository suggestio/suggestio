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
  //case object Bss extends GeoLocType("b") {
  //  override def precision        = 20
  //  override def isHighAccuracy     = false
  //  override def previous         = None
  //}

  /** Точная небыстрая геолокация по спутникам. */
  case object Gps extends GeoLocType("g") {
    override def precision        = 50
    override def isHighAccuracy   = true
    override def previous         = None //Some(Bss)
  }


  override def values = findValues

  /** Все значения модели. */
  def all = Set.empty[GeoLocType] + Gps
  /** Минимальная точность у вышек. */
  def min = Gps //Bss
  /** Максимальная точность у gps. */
  def max = Gps

}


/** Класс одного элемента модели [[GeoLocTypes]]. */
sealed abstract class GeoLocType(override val value: String) extends StringEnumEntry {

  /** Значение для HTML5 GeoLocation API highAccuracy. */
  def isHighAccuracy: Boolean

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

}

object GeoLocType {

  /** Поддержка сортировки с учетом ордеров. */
  implicit def ordering: Ordering[GeoLocType] = new Ordering[GeoLocType] {
    override def compare(x: GeoLocType, y: GeoLocType): Int = {
      x.precision - y.precision
    }
  }

  @inline implicit def univEq: UnivEq[GeoLocType] = UnivEq.derive


  implicit final class GltExt(private val glType: GeoLocType) extends AnyVal {

    /**
      * Если данному типу разрешено доминировать над нижележащими, то тут указыватеся макс.время
      * доминирования в миллисекундах. Если за это время от источника не поступило свежих новостей, то
      * подавление нижележащих источников отменяется.
      */
    def suppressorTtlMs: Option[Int] = {
      glType.previous.map( _ => 10000 )
    }

  }

}
