package models.mext.tw

import models.ISize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.15 18:45
 * Description: Поле размеров картинки присутствует в нескольких
 */
trait ImgWhOpt extends ICardArgsBase {
  override type W <: ImgWhOpt

  /** Размер картинки. */
  def imgWh: Option[ISize2di]
}


/** Wrapped-реализация [[ImgWhOpt]]. */
trait ImgWhOptWrapper extends ImgWhOpt with ICardArgsWrapper {

  /** Размер картинки. */
  override def imgWh = _cardArgsUnderlying.imgWh
}


trait ImgWhOptDflt extends ImgWhOpt {
  override def imgWh: Option[ISize2di] = None
}




