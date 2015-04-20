package models.im.make

import io.suggest.model.EnumMaybeWithName
import util.blocks.BlkImgMaker
import util.showcase.ScWideMaker

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 12:14
 * Description: Модель image maker'ов.
 * Каждый экземпляр модели -- это контроллер, занимающийся принятием решений по рендеру картинки на основе
 * исходных данных и внутреннего алгоритма.
 */
object Makers extends Enumeration with EnumMaybeWithName {

  /**
   * Абстрактный экземпляр этой модели.
   * @param strId Ключ модели и глобальный строковой идентификатор.
   */
  abstract protected sealed class Val(val strId: String) extends super.Val(strId) with IMaker {
    override def toString() = strId
  }

  /** Экспортируемый тип модели. */
  override type T = Val


  /** Рендер wide-картинки для выдачи (showcase).
    * Используется квантование ширины по линейке размеров, т.е. картинка может быть больше запрошенных размеров. */
  val ScWide = new Val("sc") with IMakerWrapper {
    override def _underlying = ScWideMaker
  }

  /** Рендерер картинки, вписывающий её точно в параметры блока. Рендерер опирается на параметры кропа,
    * заданные в редакторе карточек. */
  val Block = new Val("blk") with IMakerWrapper {
    override def _underlying = BlkImgMaker
  }

}
