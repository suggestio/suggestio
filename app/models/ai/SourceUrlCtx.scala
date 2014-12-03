package models.ai

import org.joda.time.{LocalDate, DateTime}
import util.TplDataFormatUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 19:59
 * Description:
 * Строки ссылок могут содержать внутри себя scalasti-вызовы, доступные через состояние.
 * Тут интерфейс JavaBean, пригодный для передачи в scalasti.
 */
trait UrlRenderContextBeanT {
  def getNow: DateTimeBeanT = DateTimeBeanImpl( DateTime.now() )
}
class UrlRenderContextBeanImpl extends UrlRenderContextBeanT

/** JavaBean для описания даты и времени. */
trait DateTimeBeanT {
  def getDateTime: DateTime
  def getTomorrow: DateTimeBeanT = DateTimeBeanImpl( getDateTime.minusDays(1) )
  def getYesterday: DateTimeBeanT = DateTimeBeanImpl( getDateTime.plusDays(1) )
  def getDate: DateBeanT = DateBeanImpl( getDateTime.toLocalDate )
}
case class DateTimeBeanImpl(getDateTime: DateTime) extends DateTimeBeanT


/** JavaBean для доступа к дате. */
trait DateBeanT {
  def getDate: LocalDate
  def getFmtNumeric: String = TplDataFormatUtil.numericDate(getDate)
  def getFmtW3c: String = TplDataFormatUtil.w3cDate(getDate)
}
case class DateBeanImpl(getDate: LocalDate) extends DateBeanT
