package models.im

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:51
 * Description: Модель, хранящая стандартные пиксельны плотности экранов.
 * @see [[http://www.devicepixelratio.com/]].
 */
object DevPixelRatios extends Enumeration {

  private val Some005: Option[Float] = Some(0.05F)

  /**
   * Настройки сжатия.
   * @param imQualityInt quality (0..100). Основной параметр для jpeg'ов.
   * @param chromaSubSampling Цветовая субдискретизация. 2x2 по дефолту.
   * @param imBlur Желаемая размывка.
   */
  sealed case class ImCompression(
    imQualityInt      : Int,
    chromaSubSampling : ImSamplingFactor = ImSamplingFactors.SF_2x2,
    imBlur            : Option[Float] = Some005
  ) {

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


  /**
   * Экземпляр модели.
   * @param name Название семейства экранов.
   */
  protected abstract sealed class Val(val name: String) extends super.Val(name) {
    def pixelRatio: Float

    def bgCompression: ImCompression
    def fgCompression: ImCompression = bgCompression
  }


  type DevPixelRatio = Val


  /** Единичная плотность пикселей. Некогда (до середины 2013 года) самые дефолтовые девайсы. */
  val MDPI: DevPixelRatio = new Val("MDPI") {
    override def pixelRatio: Float = 1.0F
    override val bgCompression = ImCompression(
      imQualityInt = 75,
      chromaSubSampling = ImSamplingFactors.SF_2x1
    )
  }

  // DPR=1.3 слишком дробное, и не нашло популярности на рынке устройств.

  /** Только на андройдах есть такое. */
  val HDPI: DevPixelRatio = new Val("HDPI") {
    override def pixelRatio: Float = 1.5F
    override val bgCompression = ImCompression(
      imQualityInt = 70
    )
  }

  /** Андройд-девайсы и т.ч. retina, т.е. iphone4+ и прочие яблодевайсы после 2013 г. */
  val XHDPI: DevPixelRatio = new Val("XHDPI") {
    override def pixelRatio: Float = 2.0F
    override val bgCompression = ImCompression(
      imQualityInt = 65
    )
  }

  /** На середину 2014 года, это только топовые андройды. Разрешение экрана соотвествует HD1080. */
  val DPR3: DevPixelRatio = new Val("DPR3") {
    override def pixelRatio: Float = 3.0F
    override val bgCompression = ImCompression(
      imQualityInt = 55
    )
  }


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
        (dpi.pixelRatio >= ratio) || (dpi.pixelRatio * 1.05 > ratio)
      }
      .fold [DevPixelRatio] { values.last } { v => v }
  }

}
