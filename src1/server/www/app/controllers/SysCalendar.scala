package controllers

import java.io.{ByteArrayInputStream, StringWriter}

import com.google.inject.Inject
import de.jollyday.util.XMLUtil
import de.jollyday.{HolidayCalendar, HolidayManager}
import io.suggest.util.logs.MacroLogsImpl
import models.mcal.{MCalTypes, MCalendar, MCalendars}
import models.mproj.ICommonDi
import models.req.ICalendarReq
import org.apache.commons.io.IOUtils
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.mvc.Http.MimeTypes
import util.FormUtil._
import util.acl._
import views.html.sys1.calendar._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.14 18:33
 * Description: Работа с календаре в формате jollyday в /sys/. Можно генерить календари,
 *
 * @see [[http://jollyday.sourceforge.net/index.html]]
 */
class SysCalendar @Inject() (
                              mCalendars                  : MCalendars,
                              isSuCalendar                : IsSuCalendar,
                              calendarAccessAny           : CalendarAccessAny,
                              override val mCommonDi      : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
  with IsSu
{

  import LOGGER._
  import mCommonDi._
  import calendarAccessAny.CalendarAccessAny

  /** Форма с селектом шаблона нового календаря. */
  private def newCalTplFormM = Form(
    "tplId" -> nonEmptyText(minLength = 2, maxLength = 20)
      .transform[Option[HolidayCalendar]] (
        { code =>
          try {
            Some(HolidayCalendar.valueOf(code))
          } catch {
            case ex: Exception =>
              debug("newCalTplFormM.tplId: Failed to bind form field, value = " + code, ex)
              None
          }
        },
        { _.fold("")(_.toString) }
      )
      .verifying("error.required", _.isDefined)
      .transform[HolidayCalendar] (_.get, Some.apply)
  )

  /** Форма создания/редактирования спеки календаря. */
  private def calFormM = Form(mapping(
    "name" -> nonEmptyText(minLength = 5, maxLength = 256)
      .transform(strTrimSanitizeF, strIdentityF),
    "type" -> MCalTypes.calTypeM,
    "data" -> nonEmptyText(maxLength = 20000)
      .verifying { data =>
        try {
          val stream = new ByteArrayInputStream(data.getBytes)
          try {
            new XMLUtil().unmarshallConfiguration(stream)
            true
          } finally {
            stream.close()
          }
        } catch {
          case ex: Exception =>
            error("calFormM failed to parse calendar data", ex)
            false
        }
    }
  )
  {(name, calType, data) =>
    MCalendar(
      name      = name,
      data      = data,
      calType   = calType
    )
  }
  {mcal =>
    Some((mcal.name, mcal.calType, mcal.data))
  })


  /** Отобразить список всех сохранённых календарей. */
  def showCalendars = IsSuGet.async { implicit request =>
    val createFormM = newCalTplFormM fill HolidayCalendar.RUSSIA
    mCalendars.getAll(maxResults = 500).map { cals =>
      Ok(listCalsTpl(cals, createFormM))
    }
  }

  /** Рендер страницы с заполненной формой нового календаря на основе шаблона. На странице можно выбрать шаблон.
    * Ничего никуда не сохраняется. */
  def newCalendarFromTemplateSubmit = IsSuGet.async { implicit request =>
    newCalTplFormM.bindFromRequest().fold(
      {formWithErrors =>
        val calsFut = mCalendars.getAll(maxResults = 500)
        debug("newCalendarFormTpl(): Form bind failed:\n" + formatFormErrors(formWithErrors))
        calsFut.map { cals =>
          NotAcceptable(listCalsTpl(cals, formWithErrors))
        }
      },
      {hc =>
        val codeId = hc.getId.toLowerCase
        getClass.getClassLoader.getResourceAsStream(s"holidays/Holidays_$codeId.xml") match {
          case null =>
            NotFound("Template calendar not found: " + codeId)
          case stream =>
            try {
              val sw = new StringWriter()
              IOUtils.copy(stream, sw)
              val data = sw.toString
              val stub = MCalendar(
                name      = "",
                data      = data,
                calType   = MCalTypes.default
              )
              val newFormBinded = calFormM.fill( stub )
              Ok(createCalFormTpl(newFormBinded))
            } finally {
              stream.close()
            }
        }
      }
    )
  }


  /** Сохранять в базу новый календарь. */
  def createCalendarSubmit = IsSuPost.async { implicit request =>
    calFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("createCalendarSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable(createCalFormTpl(formWithErrors))
      },
      {mcal =>
        for (mcalId <- mCalendars.save(mcal)) yield {
          Redirect(routes.SysCalendar.showCalendars())
            .flashing(FLASH.SUCCESS -> "Создан новый календарь.")
        }
      }
    )
  }


  /**
   * Редактировать календарь.
   *
   * @param calId id календаря.
   */
  def editCalendar(calId: String) = isSuCalendar.Get(calId).async { implicit request =>
    val cf = calFormM fill request.mcal
    editCalendarRespBody(calId, cf, Ok)
  }

  /** Общий код экшенов, рендерящих страницу редактирования. */
  private def editCalendarRespBody(calId: String, cf: Form[MCalendar], rs: Status)
                                  (implicit request: ICalendarReq[AnyContent]): Future[Result] = {
    rs( editCalFormTpl(request.mcal, cf) )
  }

  /**
    * Сабмит формы редактирования календаря.
    *
    * @param calId id календаря.
    */
  def editCalendarSubmit(calId: String) = isSuCalendar.Post(calId).async { implicit request =>
    calFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editCalendarSubmit($calId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        editCalendarRespBody(calId, formWithErrors, NotAcceptable)
      },
      {mcal2 =>
        val mcal3 = request.mcal.copy(
          name    = mcal2.name,
          data    = mcal2.data,
          calType = mcal2.calType
        )
        for (_ <- mCalendars.save(mcal3)) yield {
          HolidayManager.clearManagerCache()
          Redirect(routes.SysCalendar.showCalendars())
            .flashing(FLASH.SUCCESS -> "Изменения в календаре сохранены.")
        }
      }
    )
  }


  /**
   * Раздача xml-контента календаря. Это нужно для передачи календаря в HolidayManager через URL и кеширования в нём.
   * Проверка прав тут отсутствует.
   *
   * @param calId id календаря.
   * @return xml-содержимое календаря текстом.
   */
  def getCalendarXml(calId: String) = CalendarAccessAny(calId) { implicit request =>
    Ok(request.mcal.data)
      .as( MimeTypes.XML )
  }

}
