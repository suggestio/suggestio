package models.mdr

import play.api.mvc.Call

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 16:11
 * Description: Аргументы для рендера шаблона sys/freeAdvMdrTpl.
 * Для возможности рендера рекламной карточки, модель расширяет интерфейс AdBodyTplArgs.
 */
trait ISysMdrRefusePopupTplArgs {

  /** Маппинг refuse-формы. */
  def refuseFormM: RefuseForm_t

  /** Адресок для сабмита refuse-формы. */
  def submitCall: Call

}


/** Дефолтовая реализация [[ISysMdrRefusePopupTplArgs]]. */
case class MSysMdrRefusePopupTplArgs(
  override val submitCall   : Call,
  override val refuseFormM  : RefuseForm_t
)
  extends ISysMdrRefusePopupTplArgs

