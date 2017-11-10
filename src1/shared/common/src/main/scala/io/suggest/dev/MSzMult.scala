package io.suggest.dev

import io.suggest.common.html.HtmlConstants
import io.suggest.math.{IBinaryMathOp, IntMathModifiers}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.math.MathConst.Percents.PERCENTS_COUNT

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
      .inmap(apply, _.multBody)
  }

  implicit def univEq: UnivEq[MSzMult] = UnivEq.derive

  protected[dev] final val SZ_MULT_MOD = PERCENTS_COUNT * PERCENTS_COUNT

  /** Приведение натуального соотношения типа 1.0 к MSzMult(100).  */
  def fromDouble(szMultD: Double): MSzMult = {
    val pct = (szMultD * SZ_MULT_MOD).toInt
    apply( pct )
  }

}


/** Класс модели коэффициента изменения размера.
  *
  * @param multBody Размер в целочисленных долях от исходного.
  *                 Изначально, тут были проценты.
  */
case class MSzMult(multBody: Int)
  extends IntMathModifiers[MSzMult]
{

  def toIntPct = Math.round(toDouble * PERCENTS_COUNT)

  /** Вернуть float-значение. Таков был исходный SzMult_t. */
  def toFloat: Float = multBody.toFloat / MSzMult.SZ_MULT_MOD

  /** double-значение коэффициента изменения размера.
    * Именно это и надо юзать. */
  def toDouble: Double = multBody.toDouble / MSzMult.SZ_MULT_MOD

  // Пока только один int-аргумент, допускаем использование его как hash-код.
  override def hashCode = multBody
  override def toString = multBody + HtmlConstants.SLASH + MSzMult.SZ_MULT_MOD

  def withMultPc(multPc: Int) = copy(multBody = multPc)

  override protected[this] def applyMathOp(op: IBinaryMathOp[Int], arg2: Int): MSzMult = {
    withMultPc(
      op(multBody, arg2)
    )
  }

}


/** Модель примеров размеров. */
object MSzMults {

  /** Половинный размер. */
  def `0.5`   = MSzMult(MSzMult.SZ_MULT_MOD / 2)

  /** Нормальный размер. */
  def `1.0`   = MSzMult(MSzMult.SZ_MULT_MOD)

  /** Полуторный размер. */
  def `1.5`   = MSzMult(MSzMult.SZ_MULT_MOD + MSzMult.SZ_MULT_MOD/2)

  /** Удвоенный размер. */
  def `2.0`   = MSzMult(MSzMult.SZ_MULT_MOD * 2)

  /** Утроенный размер. */
  def `3.0`   = MSzMult(MSzMult.SZ_MULT_MOD * 3)


  /** Добавить в аккамуляторы все элементы кроме 3.0. */
  private def _allBut3Acc(acc0: List[MSzMult]): List[MSzMult] = {
    `0.5` ::
      `1.0` ::
      `1.5` ::
      `2.0` ::
      acc0
  }

  /** Все элементы модели. */
  def all: List[MSzMult] = {
    _allBut3Acc( `3.0` :: Nil )
  }

  /** Набор масштабов для редактора карточек. */
  def forAdEditor: List[MSzMult] = {
    _allBut3Acc(Nil)
  }

}