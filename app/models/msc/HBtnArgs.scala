package models.msc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.05.15 16:09
 * Description: Модель аргументов для рендера шаблонов кнопок строки заголовка.
 */
trait IhBtnArgs {
  def fgColor: String
  def hidden: Boolean
}


/** Дефолтовая реализация [[IhBtnArgs]]. */
case class HBtnArgs(
  fgColor: String,
  hidden: Boolean = false
)
  extends IhBtnArgs
