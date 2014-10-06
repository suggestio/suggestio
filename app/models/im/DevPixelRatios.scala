package models.im

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:51
 * Description: Модель, хранящая стандартные пиксельны плотности экранов.
 * @see [[http://www.devicepixelratio.com/]].
 */
object DevPixelRatios extends Enumeration {

  /**
   * Экземпляр модели.
   * @param name Название семейства экранов.
   * @param pixelRatio Значение плотности пикселей.
   */
  protected sealed case class Val(name: String, pixelRatio: Float, imQualityInt: Int, imBlur: Option[Float] = None)
    extends super.Val(name)
  {
    /** Значение quality. */
    def imQuality = imQualityInt.toDouble

    /** Сгенерить экземпляр quality ImOp. */
    def imQualityOp = QualityOp(imQuality)

    /** Опционально сгенерить GaussBlurOp, если требуется размывка. */
    def imGaussBlurOp: Option[GaussBlurOp] = {
      imBlur.map { blurFloat =>
        GaussBlurOp(blurFloat)
      }
    }

  }


  type DevPixelRatio = Val


  val MDPI: DevPixelRatio     = Val("MDPI",   1.0F, imQualityInt = 75, imBlur = Some(0.05F))
  // DPR=1.3 слишком дробное, и не нашло популярности на рынке устройств.
  val HDPI: DevPixelRatio     = Val("HDPI",   1.5F, imQualityInt = 61)
  val XHDPI: DevPixelRatio    = Val("XHDPI",  2.0F, imQualityInt = 50)
  val DPR3: DevPixelRatio     = Val("DPR3",   3.0F, imQualityInt = 37)

  implicit def value2val(x: Value): DevPixelRatio = x.asInstanceOf[DevPixelRatio]

  /** Дефолтовое значение DPR, когда нет другого. */
  def default = XHDPI

  /**
   * Подобрать экземпляр этого перечисления под указанный
   * @param ratio Значение плотности пикселей.
   * @return DevPixelRatio.
   */
  def forRatio(ratio: Float): DevPixelRatio = {
    values
      .find { v =>
        val dpi: DevPixelRatio = v
        (dpi.pixelRatio <= ratio) || (dpi.pixelRatio * 1.05 < ratio)
      }
      .fold [DevPixelRatio] { values.last } { v => v }
  }

}
