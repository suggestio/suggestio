package models.im

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.04.15 10:04
 * Description: Модель, описывающая программу для компрессии генерируемых картинок.
 * В основном, все эти данные относятся к JPEG.
 */


/**
 * Настройки сжатия.
 * @param quality quality (0..100). Основной параметр для jpeg'ов.
 * @param chromaSubSampling Цветовая субдискретизация. 2x2 по дефолту.
 * @param blur Желаемая размывка.
 */
case class ImCompression(
                          quality           : Int,
                          chromaSubSampling : ImSamplingFactor  = ImSamplingFactors.SF_2x2,
                          blur              : Option[Float]     = None
) {

  /** Значение quality. */
  def imQuality = quality.toDouble

  /** Сгенерить экземпляр quality ImOp. */
  def imQualityOp = QualityOp(imQuality)

  /** Опционально сгенерить GaussBlurOp, если требуется размывка. */
  def imGaussBlurOp: Option[GaussBlurOp] = {
    blur.map { blurFloat =>
      GaussBlurOp(blurFloat)
    }
  }

}

