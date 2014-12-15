package models.im

import models.ImgCrop
import org.im4java.core.IMOperation

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.14 10:24
 * Description: Различные операции кропа изображений.
 */

trait ImCropOpT extends ImOp {
  def crop: ImgCrop
  override def qsValue: String = crop.toUrlSafeStr
}


/**
 * Операция кропа изображения по абсолютным данным.
 * @param crop инфа о кропе. Ширина и длина в пикселях.
 */
case class AbsCropOp(crop: ImgCrop) extends ImCropOpT {
  override def opCode = ImOpCodes.AbsCrop

  override def addOperation(op: IMOperation): Unit = {
    op.crop(crop.width, crop.height, crop.offX, crop.offY)
  }
}


/**
 * Кроп относильно исходника.
 * @param crop Кроп, где размеры выставлены в процентах, а сдвиги - в пикселях.
 */
case class PercentSzCropOp(crop: ImgCrop) extends ImCropOpT {
  override def opCode = ImOpCodes.PercentSzCrop

  override def addOperation(op: IMOperation): Unit = {
    op.crop().addRawArgs(crop.toRelSzCropStr)
  }
}
