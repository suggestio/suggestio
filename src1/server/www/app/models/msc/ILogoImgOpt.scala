package models.msc

import io.suggest.primo.IUnderlying
import io.suggest.sc.IScApiVsn
import models.im.MImgT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.06.15 13:55
 * Description: Интерфейс для поля с картинкой логотипа (узла), подготовленной для шаблона.
 */
trait ILogoImgOpt {

  /** Данные по картинке логотипа, пригодные для рендера в ссылку.
    * None -- без логотипа. */
  def logoImgOpt: Option[MImgT]
}


/** Враппер для модели. */
trait ILogoImgOptWrapper extends ILogoImgOpt with IUnderlying {
  override def _underlying: ILogoImgOpt
  override def logoImgOpt = _underlying.logoImgOpt
}



/** Интерфейс для контейнера данных рендера, передаваемых в шаблон sc._logoTpl. */
trait ILogoRenderArgs
  extends ILogoImgOpt
  with ITitle
  with IFgColor
  with IScApiVsn
