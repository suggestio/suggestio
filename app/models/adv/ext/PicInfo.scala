package models.adv.ext

import io.suggest.adv.ext.model.im.INamedSize2di
import models.MImgSizeT
import models.blk.{SzMult_t, OneAdWideQsArgs}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.04.15 19:07
 * Description: Интерфейс экземпляров модели PicInfo. Эта модель является контейнером для рассчитанных данных
 * по картинке, которую надо отрендерить из карточки, использованной при рассчетах.
 */
trait IPicInfo extends MImgSizeT {
  /** рендерить широко? С указанной шириной. */
  def wide    : Option[OneAdWideQsArgs]
  /** Ширина оригинальная отмасштабированная. */
  def width   : Int
  /** Высота оригинальная отмасштабированная. */
  def height  : Int
  /** Множитель масштабирования оригинала. */
  def szMult  : SzMult_t
  /** Штатный размер, по которому равнялись. */
  def stdSz   : INamedSize2di
}


/** Инфа по картинке кодируется этим классом. Тут дефолтовая реализация [[IPicInfo]]. */
case class PicInfo(
  wide    : Option[OneAdWideQsArgs],
  width   : Int,
  height  : Int,
  szMult  : SzMult_t,
  stdSz   : INamedSize2di
)
  extends IPicInfo
