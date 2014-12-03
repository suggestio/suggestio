package controllers

import org.joda.time.DateTimeZone
import play.api.mvc.Result
import util.PlayLazyMacroLogsImpl
import util.acl.{IsSuperuserAiMad, IsSuperuser}
import util.ai.mad.MadAiUtil
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._, Forms._
import util.FormUtil._
import models.ai._

import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.14 18:05
 * Description: Управление системами автоматической генерации контента.
 * На момент создания здесь система заполнения карточек, живущая в MadAiUtil и её модель.
 */
object SysAi extends SysAiMadT {

  import views.html.sys1.ai._

  /** Раздача страницы с оглавлением по ai-подсистемам. */
  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }

}


/** ai.mad - работа с автогенераторами рекламных карточек. */
trait SysAiMadT extends SioController with PlayLazyMacroLogsImpl {

  import views.html.sys1.ai.mad._
  import LOGGER._

  /** Заглавная страница генераторов рекламных карточек. */
  def madIndex = IsSuperuser.async { implicit request =>
    val aisFut = MAiMad.getAll()
    aisFut map { ais =>
      Ok(madIndexTpl(ais))
    }
  }

  private def getDelimRe = "[,;\\s]+".r
  private def MERGE_DELIM = ", "

  /** Маппинг для списка content-handler'ов. */
  private def contentHandlersM(delimRe: Regex): Mapping[Seq[MAiMadContentHandler]] = {
    nonEmptyText(
      minLength = MAiMadContentHandlers.values.iterator.map(_.name.length).max,
      maxLength = 128
    )
    .transform [Seq[Option[MAiMadContentHandler]]] (
      {raw => delimRe.split(raw).iterator.map(MAiMadContentHandlers.maybeWithName).toSeq },
      { _.flatMap(identity(_)).mkString(MERGE_DELIM) }
    )
    .verifying("error.invalid", { _.forall { _.isDefined } })
    .transform [Seq[MAiMadContentHandler]] (
      { _.flatMap(identity(_)) },
      { _.map(Some.apply) }
    )
  }

  /** Маппинг для списка рендереров. */
  private def renderersM(delimRe: Regex): Mapping[Seq[MAiRenderer]] = {
    nonEmptyText(minLength = 2, maxLength = 128)
      .transform [Seq[Option[MAiRenderer]]] (
        { raw => delimRe.split(raw).iterator.map(MAiRenderers.maybeWithName).toSeq },
        { _.flatMap(identity(_)).mkString(MERGE_DELIM) }
      )
      .verifying("error.invalid", { _.forall(_.isDefined) })
      .transform [Seq[MAiRenderer]] (
        { _.flatMap(identity(_)) },
        { _.map(Some.apply) }
      )
  }

  /** Маппинг для списка целевых рекламных карточек, подлежащих обновлению. */
  private def targetAdIdsM(delimRe: Regex): Mapping[Seq[String]] = {
    nonEmptyText(minLength = 16, maxLength = 512)
      .transform [Seq[String]] (
        { raw => delimRe.split(raw).toSeq },
        { _.mkString(MERGE_DELIM) }
      )
      //.verifying("error.invalid", { _.forall(esIdM) })  // TODO Нужно валидировать каждый распарсенный элемент как esId.
  }

  /** Маппинг для формы создания/редактирования экземпляра MAiMad. */
  private def formM: Form[MAiMad] = {
    val delimRe = getDelimRe
    val m = mapping(
      "name"              -> nonEmptyText(minLength = 5, maxLength = 256)
        .transform [String] (strTrimSanitizeF, strIdentityF),
      "url"               -> urlStrM,
      "contentHandlers"   -> contentHandlersM(delimRe),
      "tplAdId"           -> esIdM,
      "renderers"         -> renderersM(delimRe),
      "targetAdIds"       -> targetAdIdsM(delimRe),
      "tz"                -> timeZoneM,
      "descr"             -> toStrOptM( text(maxLength = 512) )
    )
    {(name, url, chs, tplAdId, renderers, targetAdIds, tz, descrOpt) =>
      MAiMad(
        name = name, url = url, contentHandlers = chs, tplAdId = tplAdId, renderers = renderers,
        targetAdIds = targetAdIds, descr = descrOpt, tz = tz
      )
    }
    {maimad =>
      import maimad._
      // Изначально не было поля timezon'ы, поэтому она через Option[DTZ].
      Some((name, url, contentHandlers, tplAdId, renderers, targetAdIds, tz, descr))
    }
    Form(m)
  }


  /** Запрос страницы с формой создания генератора рекламных карточек. */
  def createMadAi = IsSuperuser { implicit request =>
    Ok(createTpl(formM))
  }

  /** Сабмит формы создания генерата рекламнах карточек. */
  def createMadAiSubmit = IsSuperuser.async { implicit request =>
    val formBinded = formM.bindFromRequest()
    lazy val logPrefix = "createMadAiSubmit(): "
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(createTpl(formWithErrors))
      },
      {maimad =>
        // Запускаем асинхронные проверки полученных данных: проверяем, что все указанные карточки существуют:
        MadAiUtil.dryRun(maimad)
          .flatMap[Result] { _ =>
            maimad.save map { savedId =>
              Redirect( routes.SysAi.madIndex() )
                .flashing("success" -> "Создано. Обновите страницу.")
            }
          }
          .recover {
            case ex: Exception =>
              debug(logPrefix + "dryRun() failed.", ex)
              val fwe = formBinded.withGlobalError(s"${ex.getClass.getSimpleName}: ${ex.getMessage}")
              NotAcceptable(createTpl(fwe))
          }
      }
    )
  }

  /** Запрос страницы редактирования ранее сохранённого [[models.ai.MAiMad]]. */
  def editMadAi(aiMadId: String) = IsSuperuserAiMad(aiMadId) { implicit request =>
    import request.aiMad
    val form = formM.fill(aiMad)
    Ok(editTpl(aiMad, form))
  }

  /** Сабмит формы редактирования существующей [[models.ai.MAiMad]]. */
  def editMadAiSubmit(aiMadId: String) = IsSuperuserAiMad(aiMadId).async { implicit request =>
    import request.aiMad
    val formBinded = formM.bindFromRequest()
    lazy val logPrefix = s"editMadAiSubmit($aiMadId): "
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(editTpl(aiMad, formWithErrors))
      },
      {aim1 =>
        // Обновляем исходный экземпляр MAiMad новыми данными
        val aim2 = aiMad.copy(
          name        = aim1.name,
          url         = aim1.url,
          contentHandlers =  aim1.contentHandlers,
          tplAdId     = aim1.tplAdId,
          renderers   = aim1.renderers,
          targetAdIds = aim1.targetAdIds,
          descr       = aim1.descr
        )
        // Запускаем асинхронные проверки полученных данных: проверяем, что все указанные карточки существуют:
        MadAiUtil.dryRun(aim2)
          .flatMap[Result] { _ =>
            aim2.save map { savedId =>
              Redirect( routes.SysAi.madIndex() )
                .flashing("success" -> "Сохранено. Обновите страницу.")
            }
          }
          .recover {
            case ex: Exception =>
              debug(logPrefix + "dryRun() failed.", ex)
              val fwe = formBinded.withGlobalError(s"${ex.getClass.getSimpleName}: ${ex.getMessage}")
              NotAcceptable(editTpl(aiMad, fwe))
          }
      }
    )
  }

  /** Запуск одного [[models.ai.MAiMad]] на исполнение. Результат запроса содержит инфу о проблеме. */
  def runMadAi(aiMadId: String) = IsSuperuserAiMad(aiMadId).async { implicit request =>
    trace(s"runMadAi($aiMadId): Starting by user ${request.pwOpt}")
    MadAiUtil.run(request.aiMad)
      // Рендерим результаты работы:
      .map { res =>
        Redirect( routes.SysAi.madIndex() )
          .flashing("success" -> (request.aiMad.name + ": Выполнено без ошибок."))
      }
      .recover {
        case ex: Exception =>
          val msg = s"Failed to run MAiMad($aiMadId)"
          error(msg, ex)
          NotAcceptable(msg + ":\n" + ex.getClass.getSimpleName + ": " + ex.getMessage + "\n" + ex.getStackTraceString)
      }
  }


  /** Сабмит удаления [[models.ai.MAiMad]]. */
  def deleteMadAi(aiMadId: String) = IsSuperuserAiMad(aiMadId).async { implicit request =>
    trace(s"deleteMadAi($aiMadId): Called by superuser ${request.pwOpt}")
    request.aiMad
      .delete
      .map { isDeleted =>
        val flash = if (isDeleted)
          "success" -> "Удалено успешно. Обновите страницу."
        else
          "error"   -> "Не удалось удалить элемент."
        Redirect(routes.SysAi.madIndex())
          .flashing(flash)
      }
  }

}

