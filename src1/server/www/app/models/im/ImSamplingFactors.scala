package models.im

import enumeratum.values.{StringEnum, StringEnumEntry}
import org.im4java.core.IMOperation

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 19:12
 * Description: Chroma subsampling - это технология сжатия изображений, основанная на особенностях восприятия
 * человеческого глаза.
 * @see [[http://www.impulseadventure.com/photo/chroma-subsampling.html]]
 */

object ImSamplingFactors extends StringEnum[ImSamplingFactor] {

  /** 4:4:4 (No chroma subsampling). */
  object SF_1x1 extends ImSamplingFactor("a") {
    override def horizontal     = 1
    override def vertical       = 1
    override def isSubSampling  = false
  }

  /** 4:2:2 (1/2 chroma horiz). */
  object SF_2x1 extends ImSamplingFactor("b") {
    override def horizontal: Int = 2
    override def vertical = 1
  }

  /** 4:2:2 (1/2 chroma vert). */
  object SF_1x2 extends ImSamplingFactor("c") {
    override def horizontal = 1
    override def vertical   = 2
  }

  /** 4:2:0 (1/2 chroma horiz, 1/2 chroma vert). */
  object SF_2x2 extends ImSamplingFactor("d") {
    override def horizontal = 2
    override def vertical   = 2
  }

  override def values = findValues

}


/** Класс значения этого перечисления. */
sealed abstract class ImSamplingFactor(override val value: String) extends StringEnumEntry with ImOp {

  /** Горизонтальный ресэмпл хромы. */
  def horizontal: Int
  /** Вертикальный ресэмпл хромы. */
  def vertical: Int

  /**
    * Является ли это значение компрессией вообще? [true].
    * false - если данный метод не является сжатием.
    */
  def isSubSampling: Boolean = true

  /** Значение, кодируемое в qs-строку. */
  override final def qsValue = value

  override def opCode = ImOpCodes.SamplingFactor

  override def addOperation(op: IMOperation): Unit = {
    op.samplingFactor(horizontal.toDouble, vertical.toDouble)
  }

  override def unwrappedValue: Option[String] = {
    Some(s"${horizontal}x$vertical")
  }

}
