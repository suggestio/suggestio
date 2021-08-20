package controllers

import java.io.{ByteArrayInputStream, StringWriter}
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import de.jollyday.util.XMLUtil
import de.jollyday.{HolidayCalendar, HolidayManager}
import io.suggest.cal.m.MCalTypes
import io.suggest.es.model.EsModel
import io.suggest.n2.extra.MNodeExtras
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImplLazy
import models.mcal.MCalTypesJvm
import models.req.INodeReq
import org.apache.commons.io.IOUtils
import play.api.data.Forms._
import play.api.data._
import play.api.http.HttpErrorHandler
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
final class SysCalendar @Inject() (
                                    sioControllerApi            : SioControllerApi,
                                  )
  extends MacroLogsImplLazy
{

  import sioControllerApi._

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val isSuCalendar = injector.instanceOf[IsSuCalendar]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val mCalTypesJvm = injector.instanceOf[MCalTypesJvm]
  private lazy val calendarAccessAny = injector.instanceOf[CalendarAccessAny]
  private lazy val csrf = injector.instanceOf[Csrf]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]

  import esModel.api._


  /** Форма с селектом шаблона нового календаря. */
  private def newCalTplFormM = Form(
    "tplId" -> nonEmptyText(minLength = 2, maxLength = 20)
      .transform[Option[HolidayCalendar]] (
        { code =>
          try {
            Some(HolidayCalendar.valueOf(code))
          } catch {
            case ex: Exception =>
              LOGGER.debug("newCalTplFormM.tplId: Failed to bind form field, value = " + code, ex)
              None
          }
        },
        { _.fold("")(_.toString) }
      )
      .verifying("error.required", _.isDefined)
      .transform[HolidayCalendar] (_.get, Some.apply)
  )

  /** Форма создания/редактирования спеки календаря. */
  private def calFormM: Form[MNode] = Form(mapping(
    "name" -> nonEmptyText(minLength = 5, maxLength = 256)
      .transform(strTrimSanitizeF, strIdentityF),
    "type" -> mCalTypesJvm.calTypeM,
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
            LOGGER.error("calFormM failed to parse calendar data", ex)
            false
        }
    }
  )
  {(name, calType, data) =>
    MNode.calendar(
      id = None,
      calType = calType,
      name = name,
      data = data,
    )
  }
  {mcal =>
    val mcalExt = mcal.extras.calendar.get
    Some((mcal.meta.basic.name, mcalExt.calType, mcalExt.data))
  })

  private def _getAllCalendars() = {
    mNodes
      .dynSearch(
        new MNodeSearch {
          override def nodeTypes = MNodeTypes.Calendar :: Nil
          override def limit = 100
        }
      )
  }

  /** Отобразить список всех сохранённых календарей. */
  def showCalendars() = csrf.AddToken {
    isSu().async { implicit request =>
      val createFormM = newCalTplFormM fill HolidayCalendar.RUSSIA
      for {
        cals <- _getAllCalendars()
      } yield {
        Ok(listCalsTpl(cals, createFormM))
      }
    }
  }

  /** Рендер страницы с заполненной формой нового календаря на основе шаблона. На странице можно выбрать шаблон.
    * Ничего никуда не сохраняется. */
  def newCalendarFromTemplateSubmit() = csrf.AddToken {
    isSu().async { implicit request =>
      newCalTplFormM.bindFromRequest().fold(
        {formWithErrors =>
          val calsFut = _getAllCalendars()
          LOGGER.debug("newCalendarFormTpl(): Form bind failed:\n" + formatFormErrors(formWithErrors))
          for (cals <- calsFut) yield {
            NotAcceptable(listCalsTpl(cals, formWithErrors))
          }
        },
        {hc =>
          val codeId = hc.getId.toLowerCase
          getClass.getClassLoader.getResourceAsStream(s"holidays/Holidays_$codeId.xml") match {
            case null =>
              errorHandler.onClientError(request, NOT_FOUND, s"Template calendar not found: $codeId")
            case stream =>
              try {
                val sw = new StringWriter()
                IOUtils.copy(stream, sw, StandardCharsets.UTF_8)
                val data = sw.toString
                val stub = MNode.calendar(
                  name      = "",
                  data      = data,
                  calType   = MCalTypes.default,
                  id        = None,
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
  }


  /** Сохранять в базу новый календарь. */
  def createCalendarSubmit() = csrf.Check {
    isSu().async { implicit request =>
      calFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug("createCalendarSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
          NotAcceptable(createCalFormTpl(formWithErrors))
        },
        {mcal =>
          for (_ <- mNodes.save(mcal)) yield {
            Redirect(routes.SysCalendar.showCalendars())
              .flashing(FLASH.SUCCESS -> "Создан новый календарь.")
          }
        }
      )
    }
  }


  /**
   * Редактировать календарь.
   *
   * @param calId id календаря.
   */
  def editCalendar(calId: String) = csrf.AddToken {
    isSuCalendar(calId).async { implicit request =>
      val cf = calFormM fill request.mnode
      editCalendarRespBody(calId, cf, Ok)
    }
  }

  /** Общий код экшенов, рендерящих страницу редактирования. */
  private def editCalendarRespBody(calId: String, cf: Form[MNode], rs: Status)
                                  (implicit request: INodeReq[AnyContent]): Future[Result] = {
    rs( editCalFormTpl(request.mnode, cf) )
  }

  /**
    * Сабмит формы редактирования календаря.
    *
    * @param calId id календаря.
    */
  def editCalendarSubmit(calId: String) = csrf.Check {
    isSuCalendar(calId).async { implicit request =>
      calFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"editCalendarSubmit($calId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
          editCalendarRespBody(calId, formWithErrors, NotAcceptable)
        },
        {mcal2 =>
          val mcal3 = (
            MNode.extras
              .composeLens( MNodeExtras.calendar )
              .set( mcal2.extras.calendar ) andThen
            MNode.meta
              .composeLens( MMeta.basic )
              .composeLens( MBasicMeta.nameOpt )
              .set( mcal2.meta.basic.nameOpt )
          )(request.mnode)

          for {
            _ <- mNodes.save(mcal3)
          } yield {
            HolidayManager.clearManagerCache()
            Redirect(routes.SysCalendar.showCalendars())
              .flashing(FLASH.SUCCESS -> "Изменения в календаре сохранены.")
          }
        }
      )
    }
  }


  /**
   * Раздача xml-контента календаря. Это нужно для передачи календаря в HolidayManager через URL и кеширования в нём.
   * Проверка прав тут отсутствует.
   *
   * @param calId id календаря.
   * @return xml-содержимое календаря текстом.
   */
  def getCalendarXml(calId: String) = calendarAccessAny(calId) { implicit request =>
    Ok( request.mnode.extras.calendar.get.data )
      .as( MimeTypes.XML )
  }

}
