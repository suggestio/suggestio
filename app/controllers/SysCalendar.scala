package controllers

import util.{FormUtil, PlayMacroLogsImpl}
import util.acl.{AbstractRequestWithPwOpt, PersonWrapper, SioReqMd, IsSuperuser}
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.MCalendar
import views.html.sys1.calendar._
import play.api.data._, Forms._
import de.jollyday.{HolidayManager, HolidayCalendar}
import java.io.{ByteArrayInputStream, StringWriter}
import org.apache.commons.io.IOUtils
import FormUtil._
import de.jollyday.util.XMLUtil
import play.api.mvc.{Action, Result, Request, ActionBuilder}
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future

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
      .transform[Option[HolidayCalendar]] (
        { code =>
          try {
            Some(HolidayCalendar.valueOf(code))
          } catch {
            case ex: Exception => None
          }
        },
        { _.fold("")(_.toString) }
      )
      .verifying("error.required", _.isDefined)
      .transform[HolidayCalendar] (_.get, Some.apply)
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
    val createFormM = newCalTplFormM fill HolidayCalendar.RUSSIA
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
              val newFormBinded = calFormM fill MCalendar(name = "", data = data)
              Ok(createCalFormTpl(newFormBinded))
            } finally {
              stream.close()
            }
        }
      }
    )
  }


  /** Сохранять в базу новый календарь. */
  def createCalendarSubmit = IsSuperuser.async { implicit request =>
    calFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("createCalendarSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable(createCalFormTpl(formWithErrors))
      },
      {mcal =>
        mcal.save.map { mcalId =>
          Redirect(routes.SysCalendar.showCalendars())
            .flashing("success" -> "Создан новый календарь.")
        }
      }
    )
  }


  /**
   * Редактировать календарь.
   * @param calId id календаря.
   */
  def editCalendar(calId: String) = CanAlterCal(calId).apply { implicit request =>
    // TODO Нужно обнаруживать список узлов, которые пользуются этим календарём и уведомлять оператора об этом.
    val cf = calFormM fill request.mcal
    Ok(editCalFormTpl(request.mcal, cf))
  }

  /**
   * Сабмит формы редактирования календаря.
   * @param calId id календаря.
   */
  def editCalendarSubmit(calId: String) = CanAlterCal(calId).async { implicit request =>
    calFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editCalendarSubmit($calId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable(editCalFormTpl(request.mcal, formWithErrors))
      },
      {mcal2 =>
        val mcal1 = request.mcal
        mcal1.name = mcal2.name
        mcal1.data = mcal2.data
        mcal1.save map { _ =>
          HolidayManager.clearManagerCache()
          Redirect(routes.SysCalendar.showCalendars())
            .flashing("success" -> "Изменения в календаре сохранены.")
        }
      }
    )
  }


  /** Раздача xml-контента календаря. Это нужно для передачи календаря в HolidayManager через URL и кеширования в нём.
    * Проверка прав тут отсутствует.
    * @param calId id календаря.
    * @return xml-содержимое календаря текстом.
    */
  def getCalendarXml(calId: String) = Action.async { implicit request =>
    MCalendar.getById(calId) map {
      case Some(mcal) =>
        Ok(mcal.data).as("text/xml")
      case None =>
        NotFound
    }
  }


  /**
   * ACL для нужд календаря.
   * @param calId id календаря.
   */
  case class CanAlterCal(calId: String) extends ActionBuilder[CalendarRequest] {
    override protected def invokeBlock[A](request: Request[A], block: (CalendarRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper getFromRequest request
      if (PersonWrapper.isSuperuser(pwOpt)) {
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        MCalendar.getById(calId) flatMap {
          case Some(mcal) =>
            srmFut flatMap { srm =>
              val req1 = CalendarRequest(mcal, request, pwOpt, srm)
              block(req1)
            }

          case None =>
            NotFound("Calendar not found: " + calId)
        }
      } else {
        IsSuperuser.onUnauthFut(request, pwOpt)
      }
    }
  }

  case class CalendarRequest[A](mcal: MCalendar, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
    extends AbstractRequestWithPwOpt(request)
}
