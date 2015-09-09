package controllers.ad

import controllers.SioController
import io.suggest.common.tags.edit.TagsEditConstants
import models.MNodeTag
import play.api.data._
import util.PlayMacroLogsI
import util.acl.IsAuth
import views.html.lk.tag.edit._
import MarketAdFormUtil._
import io.suggest.ad.form.AdFormConstants.TAGS_K

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:45
 * Description: Аддон для контроллера редактора карточки (узла), добавляющий экшены поддержки тегов.
 */
object NodeTagsEdit {

  /** Маппинг формы добавления одного тега. */
  def tagAddFormM: Form[MNodeTag] = {
    Form(
      TagsEditConstants.ADD_NAME_INPUT_ID  ->  tagNameAsTagM
    )
  }

}


import NodeTagsEdit._


trait NodeTagsEdit extends SioController with PlayMacroLogsI {
  
  /**
   * Добавление тега в редакторе тегов.
   */
  def tagEditorAddTag = IsAuth { implicit request =>
    val formBinded = tagAddFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        LOGGER.debug(s"tagEditorAddTag(): Form bind failed:\n ${formatFormErrors(formWithErrors)}")
        val formRendered = _addFormTpl(formWithErrors)
        NotAcceptable( formRendered )
      },
      {mtag =>
        // TODO Рендерить весь обновлённый список тегов.
        ???
        val field = formBinded(TAGS_K)
        Ok( _oneTagTpl(field) )
      }
    )
  }

}
