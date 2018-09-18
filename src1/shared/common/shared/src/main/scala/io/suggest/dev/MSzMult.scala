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

  @inline implicit def univEq: UnivEq[MSzMult] = UnivEq.derive

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

  /** Соотношение плитки для экранов с шириной 640csspx:
    * они требуют немного уменьшить карточку и это нижний предельный случай.
    *
    * Это значение является решением уравнения сетки для ширины 640 css-пикселей:
    *
    * Значение вычислено через решение уравнения для экрана w=640csspx (pxRatio=3.0 + FullHD).
    * Уравнение было получено из формулы, описанной в getGridColsCountFor() для двух ШИРОКИХ (300csspx) колонок:
    *
    *      640 - 20x
    * 2 = -----------
    *     300x + 20x
    *
    * где x -- это искомый szMult.
    */
  def GRID_MIN_SZMULT_D = 32d/33d
  /** Для рассчёта оптимальной плитки используется этот ограничитель минимального множителя размера блоков. */
  def GRID_MIN_SZMULT = MSzMult.fromDouble( GRID_MIN_SZMULT_D )

  /** Размеры для расширения плиток выдачи. Используются для подавления пустот по бокам экрана. */
  def GRID_TILE_MULTS: List[MSzMult] = {
    List(
      // Набор допустимых значений, TODO в основном - с потолка, надо оставить только нужные.
      MSzMult.fromDouble(1.4),
      MSzMult.fromDouble(1.3),
      MSzMult.fromDouble(1.2),
      MSzMult.fromDouble(1.1),
      MSzMult.fromDouble(1.06),
      MSzMult.fromDouble(1.0),
      // Экраны с шириной 640csspx требуют немного уменьшить карточку:
      GRID_MIN_SZMULT
    )
  }


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
    // Для отладки проблем с внезапным переносом текста в контейнере при некоторых масштабах добавлен GRID_TILE_MULTS.
    // TODO Убрать GRID_TILE_MULTS когда проблемы будут решены.
    GRID_TILE_MULTS reverse_::: _allBut3Acc(Nil)
  }

}
