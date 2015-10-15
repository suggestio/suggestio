package controllers.ad

import controllers.SioController
import io.suggest.model.n2.tag.edge.MTagEdge
import models.mtag.MAddTagReplyOk
import models.{TagsMap_t, TagsEditForm_t}
import play.api.data._, Forms._
import play.api.libs.json.Json
import util.PlayMacroLogsI
import util.acl.IsAuth
import views.html.lk.ad._
import views.html.lk.tag.edit._
import MarketAdFormUtil._
import io.suggest.common.tags.edit.TagsEditConstants.NEW_TAG_FN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:45
 * Description: Аддон для контроллера редактора карточки (узла), добавляющий экшены поддержки тегов.
 */
object NodeTagsEdit {

  /** Маппинг формы добавления одного тега. */
  def tagAddFormM: TagsEditForm_t = {
    Form(tuple(
      NEW_TAG_FN  -> newTagsM,
      tagsMapKM
    ))
  }

}


import NodeTagsEdit._


trait NodeTagsEdit
  extends SioController
  with PlayMacroLogsI
  with IsAuth
{
  
  /**
   * Добавление тега в редактор тегов.
   * Этот экшен не трогает никакие текущие данные в моделях,
   * а просто анализирует теги, добавляет новый, сортирует и рендерит.
   */
  def tagEditorAddTag = IsAuth { implicit request =>
    val formBinded = tagAddFormM.bindFromRequest()
    val res = formBinded.fold(
      {formWithErrors =>
        LOGGER.debug("tagEditorAddTag(): Form bind failed:\n" + formatFormErrors(formWithErrors))
        val formHtml = _addFormTpl( formWithErrors )
        NotAcceptable( formHtml )
      },
      {case (newTags, existTags) =>
        // Залить новый тег в список существующих тегов, устранив дубликаты.
        val newExistsTags: TagsMap_t = {
          existTags ++ MTagEdge.tags2mapIterator(newTags)
        }
        // Собрать новый маппинг формы для рендера.
        val tf2 = formBinded fill (Nil, newExistsTags)
        // Собрать JSON-ответ и вернуть.
        val resp = MAddTagReplyOk(
          existHtml   = _tagsExistTpl( tf2 ),
          addFormHtml = _addFormTpl( tf2 )
        )
        // Отрендерить и вернуть результат.
        Ok( Json.toJson(resp) )
      }
    )
    res.withHeaders(CACHE_CONTROL -> "no-cache")
  }

}
