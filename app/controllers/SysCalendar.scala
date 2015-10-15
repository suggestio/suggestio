package controllers

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import models.req.SioReqMd
import org.elasticsearch.client.Client
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import util.{FormUtil, PlayMacroLogsImpl}
import util.acl._
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
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.Database
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.14 18:33
 * Description: Работа с календаре в формате jollyday в /sys/. Можно генерить календари,
 * @see [[http://jollyday.sourceforge.net/index.html]]
 */
class SysCalendar @Inject() (
  mCalendar                     : MCalendar_,
  override val messagesApi      : MessagesApi,
  db                            : Database,
  override implicit val ec      : ExecutionContext,
  implicit val esClient         : Client,
  override implicit val sn      : SioNotifierStaticClientI
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IsSuperuser
{
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
    MCalendar(
      name = name,
      data = data,
      companion = mCalendar
    )
  }
  {mcal =>
    Some((mcal.name, mcal.data))
  })


  /** Отобразить список всех сохранённых календарей. */
  def showCalendars = IsSuperuser.async { implicit request =>
    val createFormM = newCalTplFormM fill HolidayCalendar.RUSSIA
    mCalendar.getAll(maxResults = 500).map { cals =>
      Ok(listCalsTpl(cals, createFormM))
    }
  }

  /** Рендер страницы с заполненной формой нового календаря на основе шаблона. На странице можно выбрать шаблон.
    * Ничего никуда не сохраняется. */
  def newCalendarFromTemplateSubmit = IsSuperuser.async { implicit request =>
    newCalTplFormM.bindFromRequest().fold(
      {formWithErrors =>
        val calsFut = mCalendar.getAll(maxResults = 500)
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
                name = "",
                data = data,
                companion = mCalendar
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
                                  (implicit request: CalendarRequest[AnyContent]): Future[Html] = {
    val calMbcs = db.withConnection { implicit c =>
      val calMbmds = MBillMmpDaily.findForCalId(calId)
      if (calMbmds.isEmpty) {
        Nil
      } else {
        val contractIds = calMbmds.map(_.contractId)
        MBillContract.multigetByIds(contractIds)
      }
    }
    val calUsersAdnIds = calMbcs.map(_.adnId)
    val calUsersFut = MAdnNode.multiGetRev(calUsersAdnIds)
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
    mCalendar.getById(calId) map {
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
        mCalendar.getById(calId) flatMap {
          case Some(mcal) =>
            srmFut flatMap { srm =>
              val req1 = CalendarRequest(mcal, request, pwOpt, srm)
              block(req1)
            }

          case None =>
            NotFound("Calendar not found: " + calId)
        }
      } else {
        IsSuperuser.supOnUnauthFut(request, pwOpt)
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
