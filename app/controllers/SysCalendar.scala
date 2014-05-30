package controllers

import util.{FormUtil, PlayMacroLogsImpl}
import util.acl.IsSuperuser
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.MCalendar
import views.html.sys1.calendar._
import play.api.data._, Forms._
import de.jollyday.HolidayCalendar
import java.io.{ByteArrayInputStream, StringWriter}
import org.apache.commons.io.IOUtils
import FormUtil._
import de.jollyday.util.XMLUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.14 18:33
 * Description: Работа с календаре в формате jollyday в /sys/. Можно генерить календари,
 * @see [[http://jollyday.sourceforge.net/index.html]]
 */
object SysCalendar extends SioController with PlayMacroLogsImpl {
  import LOGGER._

  /** Форма с селектом шаблона нового календаря. */
  private val newCalTplFormM = Form(
    "tplId" -> nonEmptyText(minLength = 2, maxLength = 20)
      .verifying { code =>
        try {
          HolidayCalendar.valueOf(code)
          true
        } catch {
          case ex: Exception => false
        }
      }
  )

  /** Форма создания/редактирования спеки календаря. */
  private val calFormM = Form(mapping(
    "name" -> nonEmptyText(minLength = 5, maxLength = 256)
      .transform(strTrimSanitizeF, strIdentityF)
    ,
    "data" -> nonEmptyText(maxLength = 20000)
      .verifying { data =>
        try {
          val stream = new ByteArrayInputStream(data.getBytes)
          try {
            new XMLUtil().unmarshallConfiguration(stream)
            true
          } catch {
            case ex: Exception =>
              warn("Failed to parse calendar", ex)
              false
          } finally {
            stream.close()
          }
        }
    }
  )
  {(name, data) =>
    MCalendar(name = name, data = data)
  }
  {mcal =>
    Some((mcal.name, mcal.data))
  })


  /** Отобразить список всех сохранённых календарей. */
  def showCalendars = IsSuperuser.async { implicit request =>
    val createFormM = newCalTplFormM.fill(HolidayCalendar.RUSSIA.getId)
    MCalendar.getAll(maxResults = 500).map { cals =>
      Ok(listCalsTpl(cals, createFormM))
    }
  }

  /** Рендер страницы с заполненной формой нового календаря на основе шаблона. На странице можно выбрать шаблон.
    * Ничего никуда не сохраняется. */
  def newCalendarFromTemplateSubmit = IsSuperuser.async { implicit request =>
    newCalTplFormM.bindFromRequest().fold(
      {formWithErrors =>
        val calsFut = MCalendar.getAll(maxResults = 500)
        debug("newCalendarFormTpl(): Form bind failed:\n" + formatFormErrors(formWithErrors))
        calsFut.map { cals =>
          NotAcceptable(listCalsTpl(cals, formWithErrors))
        }
      },
      {code =>
        val code1 = code.toLowerCase
        getClass.getClassLoader.getResourceAsStream(s"holidays/Holidays_$code1.xml") match {
          case null =>
            NotFound("Template calendar not found: " + code1)
          case stream =>
            try {
              val sw = new StringWriter()
              IOUtils.copy(stream, sw)
              val data = sw.toString
              val newFormBinded = calFormM fill MCalendar(name = "", data = data)
              Ok(calFormTpl(newFormBinded))
            } finally {
              stream.close()
            }
        }
      }
    )
  }


  /** Сохранять в базу новый календарь. */
  def createCalendarSubmit = IsSuperuser.async { implicit request =>
    ???
  }

}
