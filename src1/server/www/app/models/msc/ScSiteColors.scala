package models.msc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 18:35
 * Description:
 */
trait IScSiteColors {
   /** Цвет фона выдачи. */
  def bgColor       : String
  /** Цвет элементов выдачи. */
  def fgColor       : String

  override def toString = s"${getClass.getSimpleName}(bg=$bgColor,fg=$fgColor)"
}

case class ScSiteColors(bgColor: String, fgColor: String) extends IScSiteColors