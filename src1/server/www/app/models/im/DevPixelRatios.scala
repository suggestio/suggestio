package models.im

import enumeratum.values._
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.playx.FormMappingUtil
import japgolly.univeq.UnivEq

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:51
 * Description: Модель, хранящая стандартные пиксельны плотности экранов для разных режимов картинки.
 * @see [[http://www.devicepixelratio.com/]].
 */
case object DevPixelRatios extends ShortEnum[DevPixelRatio] {

  /** Единичная плотность пикселей. Некогда (до середины 2013 года) самые дефолтовые девайсы. */
  case object MDPI extends DevPixelRatio(10) {
    override def pixelRatio: Float = 1.0F

    // Используется SF_1x1 (т.е. откл.), иначе на контрастных переходах появляются заметные "тучи" на монотонных кусках.
    override def bgCompression = ImCompression(82)
    override def fgCompression = ImCompression(89)
  }


  // DPR=1.3 слишком дробное, и не нашло популярности на рынке устройств.


  /** Только на андройдах есть такое. */
  case object HDPI extends DevPixelRatio(15) {
    override def pixelRatio: Float = 1.5F
    override def bgCompression = ImCompression(75)
    override def fgCompression = ImCompression(83)
  }


  /** Андройд-девайсы и т.ч. retina, т.е. iphone4+ и прочие яблодевайсы после 2013 г. */
  case object XHDPI extends DevPixelRatio(20) {
    override def pixelRatio: Float = 2.0F
    // Тут blur нельзя - слишком видно.
    override val bgCompression = ImCompression(65)
    override val fgCompression = ImCompression(75)
  }


  /** На середину 2014 года, это только топовые андройды. Разрешение экрана соотвествует HD1080. */
  case object DPR3 extends DevPixelRatio(30) {
    override def pixelRatio: Float = 3.0F
    override val bgCompression = ImCompression(39)
    override val fgCompression = ImCompression(45)
  }


  override val values = findValues

  /**
   * Дефолтовое значение DPR, когда нет другого.
   *
   * @return 2.0, т.к. клиентский браузер браузер может не сообщать свои пиксели в ЛК.
   */
  def default: DevPixelRatio = XHDPI

  // Запретить авто.использование 1.0 и 1.5. Удалить полностью пока нельзя из-за зависимостей в коде.
  val valuesDetectable = XHDPI :: DPR3 :: Nil

  /**
   * Подобрать экземпляр этого перечисления под указанный
   * @param ratio Значение плотности пикселей.
   * @return DevPixelRatio.
   */
  def forRatio(ratio: Float): DevPixelRatio = {
    valuesDetectable
      .find { v =>
        (v.pixelRatio >= ratio) || (v.pixelRatio * 1.1 > ratio)
      }
      .getOrElse(valuesDetectable.last)
  }


  /** Если pixel ratio не задан, то взять дефолтовый, используемый для bgImg. */
  def pxRatioDefaulted(pxRatioOpt: Option[DevPixelRatio]): DevPixelRatio = {
    if (pxRatioOpt.isDefined) pxRatioOpt.get else default
  }

}


sealed abstract class DevPixelRatio(override val value: Short) extends ShortEnumEntry {
  //def name: String
  def pixelRatio: Float = value / 10

  def bgCompression: ImCompression
  def fgCompression: ImCompression
}


object DevPixelRatio {

  implicit def univEq: UnivEq[DevPixelRatio] = UnivEq.derive

  def mappingOpt = EnumeratumJvmUtil.shortIdOptMapping( DevPixelRatios )
  def mapping = FormMappingUtil.optMapping2required( mappingOpt )

}

