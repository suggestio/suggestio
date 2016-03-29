package controllers.ctag

import controllers.SioController
import models.mtag.{MTagBinded, MAddTagReplyOk, MTagsAddFormBinded}
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
{

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

}
