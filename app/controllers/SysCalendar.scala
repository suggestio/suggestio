package controllers

import play.twirl.api.HtmlFormat
import util.{FormUtil, PlayMacroLogsImpl}
import util.acl._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import models._
import views.html.sys1.calendar._
import play.api.data._, Forms._
import de.jollyday.{HolidayManager, HolidayCalendar}
import java.io.{ByteArrayInputStream, StringWriter}
import org.apache.commons.io.IOUtils
import FormUtil._
import de.jollyday.util.XMLUtil
import play.api.mvc._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.db.DB
import scala.Some
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.14 18:33
 * Description: Работа с календаре в формате jollyday в /sys/. Можно генерить календари,
 * @see [[http://jollyday.sourceforge.net/index.html]]
 */
object SysCalendar extends SioControllerImpl with PlayMacroLogsImpl {
  import LOGGER._

  /** Форма с селектом шаблона нового календаря. */
  private def newCalTplFormM = Form(
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
  private def calFormM = Form(mapping(
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
        } catch {
          case ex: Exception => false
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
  def editCalendar(calId: String) = CanAlterCal(calId).async { implicit request =>
    val cf = calFormM fill request.mcal
    editCalendarRespBody(calId, cf)
      .map(Ok(_))
  }

  /** Общий код экшенов, рендерящих страницу редактирования. */
  private def editCalendarRespBody(calId: String, cf: Form[MCalendar])
                                  (implicit request: CalendarRequest[AnyContent]): Future[HtmlFormat.Appendable] = {
    val calMbcs = DB.withConnection { implicit c =>
      val calMbmds = MBillMmpDaily.findForCalId(calId)
      if (calMbmds.isEmpty) {
        Nil
      } else {
        val contractIds = calMbmds.map(_.contractId)
        MBillContract.multigetByIds(contractIds)
      }
    }
    val calUsersAdnIds = calMbcs.map(_.adnId)
    val calUsersFut = MAdnNode.multiGet(calUsersAdnIds)
    calUsersFut map { calUsers =>
      editCalFormTpl(request.mcal, cf, calUsers)
    }
  }

  /**
   * Сабмит формы редактирования календаря.
   * @param calId id календаря.
   */
  def editCalendarSubmit(calId: String) = CanAlterCal(calId).async { implicit request =>
    calFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editCalendarSubmit($calId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        editCalendarRespBody(calId, formWithErrors)
          .map(NotAcceptable(_))
      },
      {mcal2 =>
        val mcal3 = request.mcal.copy(
          name = mcal2.name,
          data = mcal2.data
        )
        mcal3.save map { _ =>
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


  /** ACL для нужд календаря. */
  sealed trait CanAlterCalBase extends ActionBuilder[CalendarRequest] {
    def calId: String
    override def invokeBlock[A](request: Request[A], block: (CalendarRequest[A]) => Future[Result]): Future[Result] = {
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

  /**
   * Реализация CanAlterCalBase с поддержкой [[util.acl.ExpireSession]].
   * @param calId id календаря.
   */
  sealed case class CanAlterCal(calId: String) extends CanAlterCalBase with ExpireSession[CalendarRequest]

  sealed case class CalendarRequest[A](mcal: MCalendar, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
    extends AbstractRequestWithPwOpt(request)
}
