package models.im

import io.suggest.model.EnumValue2Val
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.14 13:51
 * Description: Модель, хранящая стандартные пиксельны плотности экранов для разных режимов картинки.
 * @see [[http://www.devicepixelratio.com/]].
 */
object DevPixelRatios extends Enumeration with EnumValue2Val {

  /**
   * Экземпляр модели.
   * @param name Название семейства экранов.
   */
  protected abstract sealed class Val(val name: String) extends super.Val(name) with IDevPixelRatio /*{
    val bgCompression: ImCompression
    val fgCompression: ImCompression
  }*/


  override type T = Val


  /** Единичная плотность пикселей. Некогда (до середины 2013 года) самые дефолтовые девайсы. */
  val MDPI: T = new Val("MDPI") {
    override def pixelRatio: Float = 1.0F

    // Используется SF_1x1 (т.е. откл.), иначе на контрастных переходах появляются заметные "тучи" на монотонных кусках.
    override val bgCompression = ImCompression(
      name = name,
      mode = "bg",
      qDflt = 88,
      chromaSsDflt = ImSamplingFactors.SF_1x1
    )
    override val fgCompression = ImCompression(
      name = name,
      mode = "fg",
      qDflt = 93,
      chromaSsDflt = ImSamplingFactors.SF_1x1
    )
  }


  // DPR=1.3 слишком дробное, и не нашло популярности на рынке устройств.


  /** Только на андройдах есть такое. */
  val HDPI: T = new Val("HDPI") {
    override def pixelRatio: Float = 1.5F
    override val bgCompression = ImCompression(
      name = name,
      mode = "bg",
      qDflt = 83,
      chromaSsDflt = ImSamplingFactors.SF_1x1
    )
    override val fgCompression = ImCompression(
      name = name,
      mode = "fg",
      qDflt = 88,
      chromaSsDflt = ImSamplingFactors.SF_1x1
    )
  }


  /** Андройд-девайсы и т.ч. retina, т.е. iphone4+ и прочие яблодевайсы после 2013 г. */
  val XHDPI: T = new Val("XHDPI") {
    override def pixelRatio: Float = 2.0F
    override val bgCompression = ImCompression(
      name = name,
      mode = "bg",
      qDflt = 70,
      chromaSsDflt = ImSamplingFactors.SF_1x1
    )
    override val fgCompression = ImCompression(
      name = name,
      mode = "fg",
      qDflt = 75,
      chromaSsDflt = ImSamplingFactors.SF_1x1
    )
  }


  /** На середину 2014 года, это только топовые андройды. Разрешение экрана соотвествует HD1080. */
  val DPR3: T = new Val("DPR3") {
    override def pixelRatio: Float = 3.0F
    override val bgCompression = ImCompression(
      name = name,
      mode = "bg",
      qDflt = 64,
      chromaSsDflt = ImSamplingFactors.SF_1x2
    )
    override val fgCompression = ImCompression(
      name = name,
      mode = "fg",
      qDflt = 68,
      chromaSsDflt = ImSamplingFactors.SF_1x2
    )
  }


  /** 
   * Дефолтовое значение DPR, когда нет другого.
   * @return 1.0, т.к. если клиентский браузер не сообщает плотность пикселей, то значит там античный мусор
   *         с плотностью 1.0
   */
  def default = MDPI

  /**
   * Подобрать экземпляр этого перечисления под указанный
   * @param ratio Значение плотности пикселей.
   * @return DevPixelRatio.
   */
  def forRatio(ratio: Float): T = {
    values
      .find { v =>
        val dpi: T = v
        (dpi.pixelRatio >= ratio) || (dpi.pixelRatio * 1.1 > ratio)
      }
      .fold [T] { values.last } { v => v }
  }


  /** Если pixel ratio не задан, то взять дефолтовый, используемый для bgImg. */
  def pxRatioDefaulted(pxRatioOpt: Option[T]): T = {
    if (pxRatioOpt.isDefined) pxRatioOpt.get else default
  }

}


object ImCompression {

  /**
   * Сборка экземпляра ImCompression с использованием конфига.
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
sealed case class ImCompression(
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

/** Интерфейс экземпляров модели [[DevPixelRatios]]. */
trait IDevPixelRatio {
  def name: String
  def pixelRatio: Float

  def bgCompression: ImCompression
  def fgCompression: ImCompression
}

