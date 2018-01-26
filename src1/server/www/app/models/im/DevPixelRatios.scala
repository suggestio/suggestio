package models.im

import io.suggest.common.menum.EnumValue2Val
import util.FormUtil.StrEnumFormMappings

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:51
 * Description: Модель, хранящая стандартные пиксельны плотности экранов для разных режимов картинки.
 * @see [[http://www.devicepixelratio.com/]].
 */
object DevPixelRatios extends Enumeration with EnumValue2Val with StrEnumFormMappings {

  /**
   * Экземпляр модели.
   * @param name Название семейства экранов.
   */
  protected abstract sealed class Val(val name: String)
    extends super.Val(name)
    with IDevPixelRatio
  {
    override final def toString() = pixelRatio.toString
  }


  override type T = Val

  /** Единичная плотность пикселей. Некогда (до середины 2013 года) самые дефолтовые девайсы. */
  val MDPI: T = new Val("MDPI") {
    override def pixelRatio: Float = 1.0F

    // Используется SF_1x1 (т.е. откл.), иначе на контрастных переходах появляются заметные "тучи" на монотонных кусках.
    override def bgCompression = ImCompression(88, ImSamplingFactors.SF_1x1)
    override def fgCompression = ImCompression(93, ImSamplingFactors.SF_1x1)
  }


  // DPR=1.3 слишком дробное, и не нашло популярности на рынке устройств.


  /** Только на андройдах есть такое. */
  val HDPI: T = new Val("HDPI") {
    override def pixelRatio: Float = 1.5F
    override def bgCompression = ImCompression(83, ImSamplingFactors.SF_1x1)
    override def fgCompression = ImCompression(88, ImSamplingFactors.SF_1x1)
  }


  /** Андройд-девайсы и т.ч. retina, т.е. iphone4+ и прочие яблодевайсы после 2013 г. */
  val XHDPI: T = new Val("XHDPI") {
    override def pixelRatio: Float = 2.0F
    override val bgCompression = ImCompression(70, ImSamplingFactors.SF_1x1)
    override val fgCompression = ImCompression(75, ImSamplingFactors.SF_1x1)
  }


  /** На середину 2014 года, это только топовые андройды. Разрешение экрана соотвествует HD1080. */
  val DPR3: T = new Val("DPR3") {
    override def pixelRatio: Float = 3.0F
    override val bgCompression = ImCompression(64, ImSamplingFactors.SF_1x2)
    override val fgCompression = ImCompression(68, ImSamplingFactors.SF_1x2)
  }


  /** 
   * Дефолтовое значение DPR, когда нет другого.
   * @return 2.0, т.к. клиентский браузер браузер может не сообщать свои пиксели в ЛК.
   */
  def default = XHDPI

  // Запретить авто.использование 1.0 и 1.5. Удалить полностью пока нельзя из-за зависимостей в коде.
  val valuesDetectable = XHDPI :: DPR3 :: Nil

  /**
   * Подобрать экземпляр этого перечисления под указанный
   * @param ratio Значение плотности пикселей.
   * @return DevPixelRatio.
   */
  def forRatio(ratio: Float): T = {
    valuesDetectable
      .find { v =>
        val dpi: T = v
        (dpi.pixelRatio >= ratio) || (dpi.pixelRatio * 1.1 > ratio)
      }
      .getOrElse(valuesDetectable.last)
  }


  /** Если pixel ratio не задан, то взять дефолтовый, используемый для bgImg. */
  def pxRatioDefaulted(pxRatioOpt: Option[T]): T = {
    if (pxRatioOpt.isDefined) pxRatioOpt.get else default
  }

  override protected def _idMaxLen: Int = 10

}


/** Интерфейс экземпляров модели [[DevPixelRatios]]. */
trait IDevPixelRatio {
  def name: String
  def pixelRatio: Float

  def bgCompression: ImCompression
  def fgCompression: ImCompression
}

