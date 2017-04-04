package models.mbill

import models.im.MImgT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 15:46
 * Description: Модель аргументов для шаблонов с инфой о daily-тарификации размещения узла.
 *
 * @see [[views.html.lk.billing._rcvrInfoTpl]]
 */

trait IRcvrInfoTplArgs extends ITfDailyTplArgsWrap {

  /** Галлерея узла. */
  def gallery   : Seq[MImgT]

  def tfArgs: ITfDailyTplArgs

  override protected final def underlying = tfArgs

}


case class MRcvrInfoTplArgs(
                             override val tfArgs     : ITfDailyTplArgs,
                             override val gallery    : Seq[MImgT]
)
  extends IRcvrInfoTplArgs
