package models.im

import models.ISize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.11.15 22:07
 * Description: Модель исчерпывающей инфы по картинке, которую можно отрендерить в шаблоне ссылку.
 */

trait IImgWithWhInfo {

  /** call для получения ссылки на картинку. */
  def mimg: MImgT

  /** Метаданные для рендера тега img. */
  def meta: ISize2di

  override def toString: String = {
    getClass.getSimpleName + "(" + mimg + "," + meta + ")"
  }

}

case class MImgWithWhInfo(
  override val mimg : MImgT,
  override val meta : ISize2di
)
  extends IImgWithWhInfo

