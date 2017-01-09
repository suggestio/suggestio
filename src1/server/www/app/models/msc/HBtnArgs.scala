package models.msc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.05.15 16:09
 * Description: Модель аргументов для рендера шаблонов кнопок строки заголовка.
 */
trait IhBtnArgs extends IFgColor {
  def hidden: Boolean
  /** Какой-то id узла, относящегося к кнопке. */
  def adnId : Option[String]
}


/** Дефолтовая реализация [[IhBtnArgs]]. */
case class HBtnArgs(
  override val fgColor  : String,
  override val hidden   : Boolean = false,
  override val adnId    : Option[String] = None
)
  extends IhBtnArgs
