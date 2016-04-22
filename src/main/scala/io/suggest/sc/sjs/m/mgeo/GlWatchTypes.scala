package io.suggest.sc.sjs.m.mgeo

import io.suggest.common.menum.LightEnumeration
import org.scalajs.dom.PositionOptions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.04.16 17:47
  * Description: Источники геолокаций, с которыми взаимодействует GeoLocFsm.
  */
object GlWatchTypes extends LightEnumeration {

  /** Класс элементов модели. */
  sealed abstract class Val extends ValT {

    /** Строковой id элемента модели. */
    def strId: String

    override def toString = strId

    /** Значение для HTML5 GeoLocation API highAccuracy. */
    def highAccuracy: Boolean

    /** Порядок точности и сортировки одновременно. У GPS наибольшая географическая точность. */
    def precision: Int

    /** Предшествующий по точности элемент. */
    def previous: Option[T]

    /** Все предшествующие по точности элементы. */
    def allPrevious: List[T] = {
      previous.fold[List[T]](Nil) { p =>
        p :: p.allPrevious
      }
    }

    /** Выдать объект position options. */
    def posOpts: PositionOptions = {
      val po = new PositionOptions
      po.enableHighAccuracy = highAccuracy
      po
    }

    /**
      * Если данному типу разрешено доминировать над нижележащими, то тут указыватеся макс.время
      * доминирования в миллисекундах. Если за это время от источника не поступило свежих новостей, то
      * подавление нижележащих источников отменяется.
      */
    def suppressorTtlMs: Option[Int]

  }

  override type T = Val

  /** Неточная быстрая геолокация по вышкам. */
  val Bss = new Val {
    override def strId            = "b"
    override def precision        = 20
    override def highAccuracy     = false
    override def previous         = None
    override def suppressorTtlMs  = None
  }

  /** Точная медленная геолокация по спутникам. */
  val Gps = new Val {
    override def strId            = "g"
    override def precision        = 50
    override def highAccuracy     = true
    override def previous         = Some(Bss)
    /** Считаем для GPS необходимость отклика в течение 10 секунд максимум. */
    override def suppressorTtlMs  = Some(10000)
  }


  /** Все значения модели. */
  def all = Set[T](Gps, Bss)
  /** Минимальная точность у вышек. */
  def min = Bss
  /** Максимальная точность у gps. */
  def max = Gps



  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Bss.strId => Some(Bss)
      case Gps.strId => Some(Gps)
      case _         => None
    }
  }


  /** Поддержка сортировки с учетом ордеров. */
  implicit def ordering: Ordering[T] = new Ordering[T] {
    override def compare(x: T, y: T): Int = {
      x.precision - y.precision
    }
  }

}
