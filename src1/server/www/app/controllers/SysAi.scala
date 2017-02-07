package controllers

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImplLazy
import models.ai._
import models.mproj.ICommonDi
import play.api.data.Forms._
import play.api.data._
import util.FormUtil._
import util.acl.{IsSu, IsSuAiMad}
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
  mAiMads                         : MAiMads,
  isSuAiMad                       : IsSuAiMad,
  isSu                            : IsSu,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImplLazy
{

  import LOGGER._
  import mCommonDi._


  /** Раздача страницы с оглавлением по ai-подсистемам. */
  def index = isSu.Get { implicit request =>
    Ok(indexTpl())
  }


  /** Заглавная страница генераторов рекламных карточек. */
  def madIndex = isSu.Get.async { implicit request =>
    val aisFut = mAiMads.getAll()
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
        { _.flatten.mkString(MERGE_DELIM) }
      )
      .verifying("error.invalid", { _.forall(_.isDefined) })
      .transform [Seq[MAiRenderer]] (
        { _.flatten },
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
  def createMadAi = isSu.Get { implicit request =>
    Ok(createTpl(formM))
  }

  /** Сабмит формы создания генерата рекламнах карточек. */
  def createMadAiSubmit = isSu.Post.async { implicit request =>
    val formBinded = formM.bindFromRequest()
    lazy val logPrefix = "createMadAiSubmit(): "
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(createTpl(formWithErrors))
      },
      {maimad =>
        // Запускаем асинхронные проверки полученных данных: проверяем, что все указанные карточки существуют:
        val fut = for {
          _ <- madAiUtil.dryRun(maimad)
          savedId <- mAiMads.save(maimad)
        } yield {
          Redirect( routes.SysAi.madIndex() )
            .flashing(FLASH.SUCCESS -> "Создано. Обновите страницу.")
        }

        // Перехватить невалидный MAdAi
        fut.recover {
          case ex: Exception =>
            debug(logPrefix + "dryRun() failed.", ex)
            val fwe = formBinded.withGlobalError(s"${ex.getClass.getSimpleName}: ${ex.getMessage}")
            NotAcceptable(createTpl(fwe))
        }
      }
    )
  }

  /** Запрос страницы редактирования ранее сохранённого [[models.ai.MAiMad]]. */
  def editMadAi(aiMadId: String) = isSuAiMad.Get(aiMadId) { implicit request =>
    import request.aiMad
    val form = formM.fill(aiMad)
    Ok(editTpl(aiMad, form))
  }

  /** Сабмит формы редактирования существующей [[models.ai.MAiMad]]. */
  def editMadAiSubmit(aiMadId: String) = isSuAiMad.Post(aiMadId).async { implicit request =>
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
        val resFut = for {
          _         <- madAiUtil.dryRun(aim2)
          _         <- mAiMads.save(aim2)
        } yield {
          Redirect( routes.SysAi.madIndex() )
            .flashing(FLASH.SUCCESS -> "Сохранено. Обновите страницу.")
        }

        resFut.recover {
          case ex: Exception =>
            debug(logPrefix + "dryRun() failed.", ex)
            val fwe = formBinded.withGlobalError(s"${ex.getClass.getSimpleName}: ${ex.getMessage}")
            NotAcceptable(editTpl(aiMad, fwe))
        }
      }
    )
  }

  /** Запуск одного [[models.ai.MAiMad]] на исполнение. Результат запроса содержит инфу о проблеме. */
  def runMadAi(aiMadId: String) = isSuAiMad(aiMadId).async { implicit request =>
    lazy val logPrefix = s"runMadAi($aiMadId):"
    trace(s"$logPrefix: Starting by user ${request.user.personIdOpt}")
    madAiUtil.run(request.aiMad)
      // Рендерим результаты работы:
      .map { res =>
        trace(s"$logPrefix Done, res = $res")
        Redirect( routes.SysAi.madIndex() )
          .flashing(FLASH.SUCCESS -> (request.aiMad.name + ": Выполнено без ошибок."))
      }
      .recover {
        case ex: Exception =>
          val msg = s"Failed to run MAiMad($aiMadId)"
          error(s"$logPrefix $msg", ex)
          NotAcceptable(msg + ":\n" + ex.getClass.getSimpleName + ": " + ex.getMessage + "\n" + ex.getStackTrace.mkString("", "\n", "\n"))
      }
  }


  /** Сабмит удаления [[models.ai.MAiMad]]. */
  def deleteMadAi(aiMadId: String) = isSuAiMad.Post(aiMadId).async { implicit request =>
    val deleteFut = mAiMads.deleteById(aiMadId)
    trace(s"deleteMadAi($aiMadId): Called by superuser ${request.user.personIdOpt}")
    for (isDeleted <- deleteFut) yield {
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

