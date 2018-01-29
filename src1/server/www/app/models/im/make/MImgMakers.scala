package models.im.make

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.playx.FormMappingUtil
import japgolly.univeq.UnivEq
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
case object MImgMakers extends StringEnum[MImgMaker] {

  /** Рендер wide-картинки для выдачи (showcase).
    * Используется квантование ширины по линейке размеров, т.е. картинка может быть больше запрошенных размеров. */
  case object ScWide extends MImgMaker("scw") {
    override def longName   = "Showcase wide"
    override def makerClass = ClassTag( classOf[ScWideMaker] )
  }

  /** Рендерер картинки, вписывающий её точно в параметры блока. Рендерер опирается на параметры кропа,
    * заданные в редакторе карточек. */
  case object Block  extends MImgMaker("blk") {
    override def longName   = "Block-sized"
    override def makerClass = ClassTag( classOf[BlkImgMaker] )
  }

  /** Жесткий wide-рендер под обязательно заданный экран. */
  case object StrictWide extends MImgMaker("strw") {
    override def longName   = "Strict wide"
    override def makerClass = ClassTag( classOf[StrictWideMaker] )
  }

  override def values = findValues


  /**
   * У focused-отображения карточки два варианта рендера.
 *
   * @param isWide true, если требуется широкий рендер. Иначе false.
   * @return Maker, подходящий под описанные условия.
   */
  def forFocusedBg(isWide: Boolean): MImgMaker = {
    if (isWide)
      ScWide
    else
      Block
  }

}


/** Класс одного элемента модели MImgMaker. */
sealed abstract class MImgMaker(override val value: String) extends StringEnumEntry {

  /** Длинное имя, отображаемое юзеру. */
  def longName: String

  /** Инфа для инжекции инстанса maker'а. */
  def makerClass: ClassTag[IImgMaker]

}

object MImgMaker {

  def univEq: UnivEq[MImgMaker] = UnivEq.derive

  def mappingOpt = EnumeratumJvmUtil.stringIdOptMapping( MImgMakers )
  def mapping = FormMappingUtil.optMapping2required( mappingOpt )

}
