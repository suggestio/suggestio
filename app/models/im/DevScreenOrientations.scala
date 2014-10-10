package models.im

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.14 17:24
 * Description: Типы ориентации экранов клиентских устройств.
 * 2014.oct.10: Эта модель по сути нужна только для статистики.
 */
object DevScreenOrientations extends Enumeration {

  protected case class Val(strId: String) extends super.Val(strId)

  type DevScreenOrientation = Val

  val VERTICAL: DevScreenOrientation    = Val("v")
  val HORIZONTAL: DevScreenOrientation  = Val("h")
  val SQUARE: DevScreenOrientation      = Val("sq")

}
