package util.img

import io.suggest.img.{PicCrop, ConvertModes, SioImageUtilT}
import io.suggest.util.MacroLogsImpl
import play.api.Play.current
import java.io.File

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 14:40
 * Description: Статический конвертер картинок, используемых для картинок к промо-офферам.
 */

object ImgPromoOfferUtil extends SioImageUtilT with MacroLogsImpl {
  import LOGGER._

  override val DOWNSIZE_HORIZ_PX: Integer = Integer valueOf current.configuration.getInt("offer.promo.size.max.horiz.px").getOrElse(512)
  override val DOWNSIZE_VERT_PX: Integer  = Integer valueOf current.configuration.getInt("offer.promo.size.max.horiz.px").getOrElse(1024)

  /** Если на выходе получилась слишком жирная превьюшка, то отсеять её. */
  override def MAX_OUT_FILE_SIZE_BYTES: Option[Int] = None

  /** Картинка считается слишком маленькой для обработки, если хотя бы одна сторона не превышает этот порог. */
  override val MIN_SZ_PX: Int = current.configuration.getInt("offer.promo.picture.min.sz.px") getOrElse 200

  /** Если исходный jpeg после стрипа больше этого размера, то сделать resize. */
  override def MAX_SOURCE_JPEG_NORSZ_BYTES = None

  /** Качество сжатия jpeg. */
  override val JPEG_QUALITY_PC: Double = current.configuration.getDouble("offer.promo.jpeg.quality") getOrElse 85.0

}
