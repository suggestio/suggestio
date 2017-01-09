package models.im

import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.04.15 10:04
 * Description: Модель, описывающая программу для компрессии генерируемых картинок.
 * В основном, все эти данные относятся к JPEG.
 */

object ImCompression {

  /**
   * Сборка экземпляра ImCompression с использованием конфига и плотности пикселей клиентского устройства.
   * @param name Название, например MDPI.
   * @param mode Режим: fg, bg или иные.
   * @param qDflt Дефолтовое значение качества.
   * @param chromaSsDflt Дефолтовая передискретизация цветов [2x2].
   * @param imBlurDflt Дефолтованая размывка, если требуется [None].
   * @return Экземпляр ImCompression, готовый к эксплуатации.
   */
  def apply(name: String, mode: String, qDflt: Int, chromaSsDflt: ImSamplingFactor = ImSamplingFactors.SF_2x2,
            imBlurDflt: Option[Float] = None): ImCompression = {
    // Пытаемся получить параметры сжатия из конфига
    ImCompression(
      imQualityInt = configuration.getInt(s"dpr.$name.$mode.quality")
        .getOrElse(qDflt),
      chromaSubSampling = configuration.getString(s"dpr.$name.$mode.chroma.ss")
        .fold(chromaSsDflt)(ImSamplingFactors.withName),
      imBlur = configuration.getDouble(s"dpr.$name.$mode.blur.gauss")
        .map(_.toFloat)
        .orElse(imBlurDflt)
    )
  }

}


/**
 * Настройки сжатия.
 * @param imQualityInt quality (0..100). Основной параметр для jpeg'ов.
 * @param chromaSubSampling Цветовая субдискретизация. 2x2 по дефолту.
 * @param imBlur Желаемая размывка.
 */
case class ImCompression(
  imQualityInt      : Int,
  chromaSubSampling : ImSamplingFactor,
  imBlur            : Option[Float]
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

