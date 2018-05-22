package models.im

import io.suggest.dev.{MPxRatio, MPxRatios}
import io.suggest.img.{MImgFmt, MImgFmts}
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.04.15 10:04
 * Description: Модель, описывающая программу для компрессии генерируемых картинок.
 * В основном, все эти данные относятся к JPEG.
 */

object ImCompression {

  val SOME_BLUR = Some( GaussBlurOp(1) )
  val SOME_CHROMA_SS = Some( ImSamplingFactors.Sf1x2 )


  def bgForPxRatio(dpr: MPxRatio): ImCompression = {
    dpr match {
      case MPxRatios.XHDPI => ImCompression(65)
      case MPxRatios.DPR3  => ImCompression(39)
      // Используется SF_1x1 (т.е. откл.), иначе на контрастных переходах появляются заметные "тучи" на монотонных кусках.
      case MPxRatios.MDPI  => ImCompression(82)
      case MPxRatios.HDPI  => ImCompression(75)
    }
  }

  def fgForPxRatio(dpr: MPxRatio): ImCompression = {
    dpr match {
      case MPxRatios.XHDPI => ImCompression(75)
      case MPxRatios.DPR3  => ImCompression(45)
      case MPxRatios.MDPI  => ImCompression(89)
      case MPxRatios.HDPI  => ImCompression(83)
    }
  }

  def forPxRatio(compressMode: CompressMode, dpr: MPxRatio): ImCompression = {
    compressMode match {
      case CompressModes.Fg =>
        fgForPxRatio(dpr)
      case CompressModes.Bg =>
        bgForPxRatio(dpr)
      case CompressModes.DeepBackground =>
        val c0 = bgForPxRatio(dpr)
        c0.copy(
          quality           = c0.quality - 10,
          chromaSubSampling = ImCompression.SOME_CHROMA_SS,
          // Размывка 1х1 для сокрытия краёв после сильного сжатия.
          blur              = ImCompression.SOME_BLUR
        )
    }
  }

}


/**
 * Настройки сжатия.
 * @param quality quality (0..100). Основной параметр для jpeg'ов.
 *                Игнорится для png/итд, т.к. там шкала наоборот или имеет иной смысл.
 * @param chromaSubSampling Цветовая субдискретизация. 2x2 по дефолту. (JPEG)
 * @param blur Желаемая размывка.
 */
case class ImCompression(
                          quality           : Int,
                          chromaSubSampling : Option[ImSamplingFactor]  = None,
                          blur              : Option[GaussBlurOp]       = None
                        ) {

  /** Сгенерить операции для сжатия.
    *
    * @return Список операций.
    */
  def toOps(outputFormat: MImgFmt): List[ImOp] = {
    var acc = List.empty[ImOp]

    // Добавляем, соблюдая порядок полей (для удобства при отладке):
    for (b <- blur)
      acc ::= b

    val isJpeg = outputFormat ==* MImgFmts.JPEG

    for (chrSs <- chromaSubSampling if isJpeg)
      acc ::= chrSs

    // Добавить quality в начало списка, чтобы был как визуальный ориентир-разделитель:
    // Нельзя выставлять q для PNG (или всегда 100 выставлять), она там работает наоборот.
    if (isJpeg)
      acc ::= QualityOp( quality )

    acc
  }

}

