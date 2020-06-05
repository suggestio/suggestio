package io.suggest.dev

import enumeratum.values.{ShortEnum, ShortEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:51
 * Description: Модель, хранящая стандартные пиксельны плотности экранов для разных режимов картинки.
 *
 * @see [[http://www.devicepixelratio.com/]].
 */
case object MPxRatios extends ShortEnum[MPxRatio] {

  /** Единичная плотность пикселей. Некогда (до середины 2013 года) самые дефолтовые девайсы. */
  case object MDPI extends MPxRatio(10) {
    override def pixelRatio: Float = 1.0F
  }


  // DPR=1.3 слишком дробное, и не нашло популярности на рынке устройств.


  /** Только на андройдах есть такое. */
  case object HDPI extends MPxRatio(15) {
    override def pixelRatio: Float = 1.5F
  }


  /** Андройд-девайсы и т.ч. retina, т.е. iphone4+ и прочие яблодевайсы после 2013 г. */
  case object XHDPI extends MPxRatio(20) {
    override def pixelRatio: Float = 2.0F
    // Тут blur нельзя - слишком видно.
  }


  /** На середину 2014 года, это только топовые андройды. Разрешение экрана соотвествует HD1080. */
  case object DPR3 extends MPxRatio(30) {
    override def pixelRatio: Float = 3.0F
  }


  override val values = findValues


  /**
   * Дефолтовое значение DPR, когда нет другого.
   *
   * @return 2.0, т.к. клиентский браузер браузер может не сообщать свои пиксели в ЛК.
   */
  def default: MPxRatio = XHDPI

  // Запретить авто.использование 1.0 и 1.5. Удалить полностью пока нельзя из-за зависимостей в коде.
  def valuesDetectable = XHDPI :: DPR3 :: Nil


  /**
   * Подобрать экземпляр этого перечисления под указанный
   * @param ratio Значение плотности пикселей.
   * @return DevPixelRatio.
   */
  def forRatio(ratio: Float): MPxRatio = {
    val vds = valuesDetectable
    vds
      .find { v =>
        (v.pixelRatio >= ratio) || (v.pixelRatio * 1.1 > ratio)
      }
      .getOrElse( vds.last )
  }
  def forRatio(ratio: Double): MPxRatio = {
    forRatio( ratio.toFloat )
  }


  /** Если pixel ratio не задан, то взять дефолтовый, используемый для bgImg. */
  def pxRatioDefaulted(pxRatioOpt: Option[MPxRatio]): MPxRatio = {
    if (pxRatioOpt.isDefined) pxRatioOpt.get
    else default
  }

}


sealed abstract class MPxRatio(override val value: Short) extends ShortEnumEntry {
  def pixelRatio: Float = value / 10
}

object MPxRatio {

  @inline implicit def univEq: UnivEq[MPxRatio] = UnivEq.derive

  implicit def devPixelRatioFormat: Format[MPxRatio] = {
    EnumeratumUtil.valueEnumEntryFormat( MPxRatios )
  }

}
