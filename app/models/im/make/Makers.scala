package models.im.make

import io.suggest.common.menum.EnumMaybeWithName
import util.FormUtil.StrEnumFormMappings
import util.blocks.BlkImgMaker
import util.img.StrictWideMaker
import util.showcase.ScWideMaker

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 12:14
 * Description: Модель image maker'ов.
 * Каждый экземпляр модели -- это контроллер, занимающийся принятием решений по рендеру картинки на основе
 * исходных данных и внутреннего алгоритма.
 */
object Makers extends EnumMaybeWithName with StrEnumFormMappings {

  /**
   * Абстрактный экземпляр этой модели.
   * @param strId Ключ модели и глобальный строковой идентификатор.
   */
  abstract protected sealed class Val(val strId: String)
    extends super.Val(strId)
    with IMaker
  {
    override def toString() = strId
    /** Длинное имя, отображаемое юзеру. */
    def longName: String
  }

  /** Экспортируемый тип модели. */
  override type T = Val


  /** Рендер wide-картинки для выдачи (showcase).
    * Используется квантование ширины по линейке размеров, т.е. картинка может быть больше запрошенных размеров. */
  val ScWide = new Val("scw") with IMakerWrapper {
    override def _underlying = ScWideMaker
    override def longName = "Showcase wide"
  }

  /** Рендерер картинки, вписывающий её точно в параметры блока. Рендерер опирается на параметры кропа,
    * заданные в редакторе карточек. */
  val Block = new Val("blk") with IMakerWrapper {
    override def _underlying = BlkImgMaker
    override def longName = "Block-sized"
  }

  /** Жесткий wide-рендер под обязательно заданный экран. */
  val StrictWide = new Val("strw") with IMakerWrapper {
    override def _underlying = StrictWideMaker
    override def longName = "Strict wide"
  }

  override protected def _idMaxLen: Int = 6


  /**
   * У focused-отображения карточки два варианта рендера.
   * @param isWide true, если требуется широкий рендер. Иначе false.
   * @return Maker, подходящий под описанные условия.
   */
  def forFocusedBg(isWide: Boolean): T = {
    if (isWide)
      ScWide
    else
      Block
  }

}
