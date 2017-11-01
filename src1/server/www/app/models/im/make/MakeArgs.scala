package models.im.make

import io.suggest.common.geom.d2.ISize2di
import models.blk.SzMult_t
import models.im.{CompressMode, DevScreen, MImgT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 12:18
 * Description: Аргументы для запроса данных для рендера
 */
trait IMakeArgs {

  /** Параметры экрана устройства. Берется из Context или сочиняется вручную.
    * None значит необходимые дефолтовые данные подставляет конкретная реализация [[IMaker]]. */
  def devScreenOpt: Option[DevScreen]

  /** Картинка для обработки. У рекламной карточки в теории может быть несколько картинок. */
  def img: MImgT

  /** Метаданные исходного блока или контейнера для отображения картинки.
    * А вообще тут размеры объекта, к которому относится эта картинка. */
  def blockMeta: ISize2di

  /** Коэфф. масштабирования. */
  def szMult: SzMult_t

  /** Используемая программа компрессии картинки вместо дефолтовой. */
  def compressMode: Option[CompressMode]

}


/** Дефолтовая реализация [[IMakeArgs]]. */
case class MakeArgs(
  override val img          : MImgT,
  override val blockMeta    : ISize2di,
  override val szMult       : SzMult_t,
  override val devScreenOpt : Option[DevScreen],
  override val compressMode : Option[CompressMode] = None
)
  extends IMakeArgs
