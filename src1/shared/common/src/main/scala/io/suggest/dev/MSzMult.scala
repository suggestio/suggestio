package io.suggest.dev

import io.suggest.common.html.HtmlConstants
import io.suggest.math.{IBinaryMathOp, IntMathModifiers}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.math.MathConst.PERCENTS_COUNT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.17 17:40
  * Description: Кросс-платформенная модель мультипликатора размера элементов при рендере.
  * Из-за визуальной составляющей рендера и кросс-платформенности, модель оперирует
  * целочисленными процентами, а не числами с плавающей точкой.
  */
object MSzMult {

  /** Поддержка play-json. */
  implicit val MSZ_MULT_FORMAT: OFormat[MSzMult] = {
    (__ \ "m").format[Int]
      .inmap(apply, _.multPct)
  }

  implicit def univEq: UnivEq[MSzMult] = UnivEq.derive

}


/** Класс модели коэффициента изменения размера.
  *
  * @param multPct Размер в процентах от исходного.
  *                Например 100 означает масштаб 100%.
  */
case class MSzMult(multPct: Int)
  extends IntMathModifiers[MSzMult]
{

  /** Вернуть float-значение. Таков был исходный SzMult_t. */
  def toFloat: Float = multPct.toFloat / PERCENTS_COUNT

  /** double-значение коэффициента изменения размера.
    * Именно это и надо юзать. */
  def toDouble: Double = multPct.toDouble / PERCENTS_COUNT

  // Пока только один int-аргумент, допускаем использование его как hash-код.
  override def hashCode = multPct
  override def toString = multPct + HtmlConstants.SLASH + PERCENTS_COUNT

  def withMultPc(multPc: Int) = copy(multPct = multPc)

  override protected[this] def applyMathOp(op: IBinaryMathOp[Int], arg2: Int): MSzMult = {
    withMultPc(
      op(multPct, arg2)
    )
  }

}


/** Модель примеров размеров. */
object MSzMults {

  /** Половинный размер. */
  def `0.5`   = MSzMult(PERCENTS_COUNT / 2)

  /** Нормальный размер. */
  def `1.0`   = MSzMult(PERCENTS_COUNT)

  /** Полуторный размер. */
  def `1.5`   = MSzMult(PERCENTS_COUNT + PERCENTS_COUNT/2)

  /** Удвоенный размер. */
  def `2.0`   = MSzMult(PERCENTS_COUNT * 2)

  /** Утроенный размер. */
  def `3.0`   = MSzMult(PERCENTS_COUNT * 3)


  def all: List[MSzMult] = {
    `0.5` ::
      `1.0` ::
      `1.5` ::
      `2.0` ::
      `3.0` ::
      Nil
  }

  def forAdEditor: List[MSzMult] = all

}
