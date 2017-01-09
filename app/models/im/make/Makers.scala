package models.im.make

import io.suggest.common.menum.EnumMaybeWithName
import util.FormUtil.StrEnumFormMappings
import util.blocks.BlkImgMaker
import util.img.StrictWideMaker
import util.showcase.ScWideMaker

import scala.reflect.ClassTag

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.15 12:14
  * Description: Статическая модель, описывающая известные системе image maker'ы.
  *
  * Каждый экземпляр модели -- это мета-инфа по одному image-maker'у.
  * С помощью инжектора, клиенты модели могут получить доступ к инстансам мэйкеров.
  */
object Makers extends EnumMaybeWithName with StrEnumFormMappings {

  /**
   * Абстрактный экземпляр этой модели.
   * @param strId Ключ модели и глобальный строковой идентификатор.
   */
  abstract protected sealed class Val(val strId: String)
    extends super.Val(strId)
  {
    override def toString() = strId

    /** Длинное имя, отображаемое юзеру. */
    def longName: String

    /** Инфа для инжекции инстанса maker'а. */
    def makerClass: ClassTag[IMaker]

  }

  /** Экспортируемый тип модели. */
  override type T = Val


  /** Рендер wide-картинки для выдачи (showcase).
    * Используется квантование ширины по линейке размеров, т.е. картинка может быть больше запрошенных размеров. */
  val ScWide = new Val("scw") {
    override def longName   = "Showcase wide"
    override def makerClass = ClassTag( classOf[ScWideMaker] )
  }

  /** Рендерер картинки, вписывающий её точно в параметры блока. Рендерер опирается на параметры кропа,
    * заданные в редакторе карточек. */
  val Block = new Val("blk") {
    override def longName   = "Block-sized"
    override def makerClass = ClassTag( classOf[BlkImgMaker] )
  }

  /** Жесткий wide-рендер под обязательно заданный экран. */
  val StrictWide = new Val("strw") {
    override def longName   = "Strict wide"
    override def makerClass = ClassTag( classOf[StrictWideMaker] )
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
