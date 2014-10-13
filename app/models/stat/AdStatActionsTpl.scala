package models.stat

import models.Context
import play.api.i18n.Messages

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 14:12
 * Description: Статическая утиль для шаблонов, работающих со экшенами статистики.
 */
object AdStatActionsTpl {

  /** Фунция для генерации списка пар (String, String), которые описывают  */
  def adStatActionsSeq(implicit ctx: Context): Seq[(ScStatAction, String)] = {
    import ctx._
    ScStatActions.onlyBillableIter
      .map { v  =>  v -> Messages(v.i18nCode) }
      .toSeq
  }

  def adStatActionsSeqStr(implicit ctx: Context): Seq[(String, String)] = {
    import ctx._
    ScStatActions.onlyBillableIter
      .map { v =>
        val i18n = v.i18nCode
        v.toString -> Messages(i18n)
      }
      .toSeq
  }

}
