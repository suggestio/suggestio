package controllers

import com.google.inject.Inject
import models.ai._
import models.mproj.MCommonDi
import play.api.data.Forms._
import play.api.data._
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import util.FormUtil._
import util.PlayLazyMacroLogsImpl
import util.acl.{IsSuperuser, IsSuperuserAiMad}
import util.ai.mad.MadAiUtil
import views.html.sys1.ai._
import views.html.sys1.ai.mad._

import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.14 18:05
 * Description: Управление системами автоматической генерации контента.
 * На момент создания здесь система заполнения карточек, живущая в MadAiUtil и её модель.
 */
class SysAi @Inject() (
  madAiUtil                       : MadAiUtil,
  override val mCommonDi          : MCommonDi,
  implicit private val ws         : WSClient
)
  extends SioControllerImpl
  with PlayLazyMacroLogsImpl
  with IsSuperuserAiMad
  with IsSuperuser
{

  import LOGGER._
  import mCommonDi._


  /** Раздача страницы с оглавлением по ai-подсистемам. */
  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }


  /** Заглавная страница генераторов рекламных карточек. */
  def madIndex = IsSuperuser.async { implicit request =>
    val aisFut = MAiMad.getAll()
    aisFut map { ais =>
      Ok(madIndexTpl(ais))
    }
  }

  private def getDelimRe = "[,;\\s]+".r
  private def MERGE_DELIM = ", "

  /**
   * Маппер для карты источников и их парсеров.
   * Список вводится в textarea и имеет формат:
   *  URL1 парсер1 парсер2 ...
   *  URL2 парсер3 ...
   *  ...
   * В качестве разделителя можно использовать любой из [\\s].
   */
  private def sourcesM: Mapping[Seq[AiSource]] = {
    nonEmptyText(maxLength = 1024)
      .transform(
        {raw =>
          val tokRe = "\\s+".r
          raw.lines
            .map { line =>
              val tokens = tokRe.split(line).toList
              val url = tokens.head
              val chs = tokens.tail.map { chId =>
                MAiMadContentHandlers.withName(chId) : MAiMadContentHandler
              }
              AiSource(url, chs)
            }
            .toSeq
        },
        {sources =>
          val sb = new StringBuilder(256)
          sources.foreach { source =>
            sb.append(source.url)
              .append(' ')
            source.contentHandlers.foreach { ch =>
              sb.append(ch.name)
                .append(' ')
            }
            sb.append('\n')
          }
          sb.toString()
        }
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
    // Не валидируем ничего больше, т.к. вызов dryRun() при сабмите должен развеять все подозрения.
  }

  /** Маппинг для формы создания/редактирования экземпляра MAiMad. */
  private def formM: Form[MAiMad] = {
    val delimRe = getDelimRe
    val m = mapping(
      "name"              -> nonEmptyText(minLength = 5, maxLength = 256)
        .transform [String] (strTrimSanitizeF, strIdentityF),
      "sources"           -> sourcesM,
      "tplAdId"           -> esIdM,
      "renderers"         -> renderersM(delimRe),
      "targetAdIds"       -> targetAdIdsM(delimRe),
      "tz"                -> timeZoneM,
      "descr"             -> toStrOptM( text(maxLength = 512) )
    )
    {(name, sources, tplAdId, renderers, targetAdIds, tz, descrOpt) =>
      MAiMad(
        name = name, sources = sources, tplAdId = tplAdId, renderers = renderers,
        targetAdIds = targetAdIds, descr = descrOpt, tz = tz
      )
    }
    {maimad =>
      import maimad._
      // Изначально не было поля timezon'ы, поэтому она через Option[DTZ].
      Some((name, sources, tplAdId, renderers, targetAdIds, tz, descr))
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
        madAiUtil.dryRun(maimad)
          .flatMap[Result] { _ =>
            maimad.save map { savedId =>
              Redirect( routes.SysAi.madIndex() )
                .flashing(FLASH.SUCCESS -> "Создано. Обновите страницу.")
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
          sources     = aim1.sources,
          tplAdId     = aim1.tplAdId,
          renderers   = aim1.renderers,
          targetAdIds = aim1.targetAdIds,
          descr       = aim1.descr
        )
        // Запускаем асинхронные проверки полученных данных: проверяем, что все указанные карточки существуют:
        madAiUtil.dryRun(aim2)
          .flatMap[Result] { _ =>
            aim2.save map { savedId =>
              Redirect( routes.SysAi.madIndex() )
                .flashing(FLASH.SUCCESS -> "Сохранено. Обновите страницу.")
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
    trace(s"runMadAi($aiMadId): Starting by user ${request.user.personIdOpt}")
    madAiUtil.run(request.aiMad)
      // Рендерим результаты работы:
      .map { res =>
        Redirect( routes.SysAi.madIndex() )
          .flashing(FLASH.SUCCESS -> (request.aiMad.name + ": Выполнено без ошибок."))
      }
      .recover {
        case ex: Exception =>
          val msg = s"Failed to run MAiMad($aiMadId)"
          error(msg, ex)
          NotAcceptable(msg + ":\n" + ex.getClass.getSimpleName + ": " + ex.getMessage + "\n" + ex.getStackTrace.mkString("", "\n", "\n"))
      }
  }


  /** Сабмит удаления [[models.ai.MAiMad]]. */
  def deleteMadAi(aiMadId: String) = IsSuperuserAiMad(aiMadId).async { implicit request =>
    trace(s"deleteMadAi($aiMadId): Called by superuser ${request.user.personIdOpt}")
    request.aiMad
      .delete
      .map { isDeleted =>
        val flash = if (isDeleted) {
          FLASH.SUCCESS -> "Удалено успешно. Обновите страницу."
        } else {
          FLASH.ERROR   -> "Не удалось удалить элемент."
        }
        Redirect(routes.SysAi.madIndex())
          .flashing(flash)
      }
  }

}

