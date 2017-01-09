package models.im

import org.im4java.core.IMOperation

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 19:12
 * Description: Chroma subsampling - это технология сжатия изображений, основанная на особенностях восприятия
 * человеческого глаза.
 * @see [[http://www.impulseadventure.com/photo/chroma-subsampling.html]]
 */

object ImSamplingFactors extends Enumeration {

  /**
   * Класс значения этого перечисления.
   * @param qsValue Значение, кодируемое в qs-строку.
   * @param horizontal Горизонтальный ресэмпл хромы.
   * @param vertical Вертикальный ресэмпл хромы.
   * @param isSubSampling Является ли это значение компрессией вообще? [true].
   *                      false - если данный метод не является сжатием.
   */
  protected case class Val(qsValue: String, horizontal: Int, vertical: Int, isSubSampling: Boolean = true)
    extends super.Val(qsValue) with ImOp {

    override def opCode = ImOpCodes.SamplingFactor

    override def addOperation(op: IMOperation): Unit = {
      op.samplingFactor(horizontal.toDouble, vertical.toDouble)
    }

    override def unwrappedValue: Option[String] = {
      Some(s"${horizontal}x$vertical")
    }
  }

  /** Тип значения перечисления. */
  type ImSamplingFactor = Val


  /** 4:4:4 (No chroma subsampling). */
  val SF_1x1: ImSamplingFactor     = Val("a", 1, 1, isSubSampling = false)

  /** 4:2:2 (1/2 chroma horiz). */
  val SF_2x1: ImSamplingFactor     = Val("b", 2, 1)

  /** 4:2:2 (1/2 chroma vert). */
  val SF_1x2: ImSamplingFactor     = Val("c", 1, 2)

  /** 4:2:0 (1/2 chroma horiz, 1/2 chroma vert). */
  val SF_2x2: ImSamplingFactor     = Val("d", 2, 2)


  implicit def value2val(x: Value): ImSamplingFactor = x.asInstanceOf[ImSamplingFactor]

}
