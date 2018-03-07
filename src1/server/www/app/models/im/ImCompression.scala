package models.im

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

}


/**
 * Настройки сжатия.
 * @param quality quality (0..100). Основной параметр для jpeg'ов.
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

