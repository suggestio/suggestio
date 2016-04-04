package controllers.ctag

import controllers.SioController
import io.suggest.model.n2.tag.ITagSearchUtilDi
import models.mtag._
import play.api.libs.json.Json
import util.PlayMacroLogsI
import util.acl.IsAuth
import util.tags.ITagsEditFormUtilDi
import views.html.lk.tag.edit._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:45
 * Description: Аддон для контроллера редактора карточки (узла), добавляющий экшены поддержки тегов.
 */

trait NodeTagsEdit
  extends SioController
  with PlayMacroLogsI
  with IsAuth
  with ITagsEditFormUtilDi
  with ITagSearchUtilDi
{

  import mCommonDi._

  /**
   * Добавление тега в редактор тегов.
   * Этот экшен не трогает никакие текущие данные в моделях,
   * а просто анализирует теги, добавляет новый, сортирует и рендерит.
   */
  def tagEditorAddTag = IsAuth.apply { implicit request =>
    val formBinded = tagsEditFormUtil.addTagsForm.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        LOGGER.debug("tagEditorAddTag(): Form bind failed:\n" + formatFormErrors(formWithErrors))
        val formHtml = _addFormTpl(formWithErrors)
        NotAcceptable( formHtml )
      },

      {r =>
        // Собрать новый набор добавленных тегов.
        val existing2 = {
          val existingAcc = List.newBuilder[MTagBinded]
          existingAcc ++= r.existing
          existingAcc ++= {
            for (tf <- r.added.iterator) yield {
              MTagBinded(tf)
            }
          }
          existingAcc.result()
        }

        // Собрать обновлённый маппинг формы:
        val r2 = MTagsAddFormBinded(
          added     = Nil,
          existing  = existing2
        )
        val tf2 = formBinded.fill(r2)

        // Собрать JSON-ответ и вернуть.
        val resp = MAddTagReplyOk(
          addFormHtml = _addFormTpl  (tf2),
          existHtml   = _tagsExistTpl(tf2)
        )

        // Отрендерить и вернуть результат.
        Ok( Json.toJson(resp) )
          .withHeaders(CACHE_CONTROL -> "no-cache")
      }
    )
  }

  /** Экшен поиска похожих тегов во время набора названия.
    *
    * @return JSON с inline-версткой для отображения в качестве выпадающего списка.
    */
  def tagsSearch(tsearch: MTagSearch) = IsAuth.async { implicit request =>
    for {
      found <-  tagSearchUtil.liveSearchTagByName( tsearch.toEsSearch )
    } yield {
      if (found.tags.isEmpty) {
        NoContent
      } else {
        val resp = MTagSearchResp(
          rendered    = Some(_tagsHintTpl(found)),
          foundCount  = found.tags.size
        )
        Ok( Json.toJson(resp) )
      }
    }
  }

}
