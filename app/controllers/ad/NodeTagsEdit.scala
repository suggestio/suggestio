package controllers.ad

import controllers.SioController
import io.suggest.common.tags.edit.TagsEditConstants
import models.MNodeTag
import play.api.data._
import util.PlayMacroLogsI
import util.acl.IsAuth
import views.html.lk.tag.edit._
import MarketAdFormUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:45
 * Description: Аддон для контроллера редактора карточки (узла), добавляющий экшены поддержки тегов.
 */
object NodeTagsEdit {

  /** Маппинг формы добавления одного тега. */
  def tagAddFormM: Form[MNodeTag] = {
    val k = TagsEditConstants.ADD_NAME_INPUT_ID
    val m = tagNameAsTagM
    Form(k -> m)
  }

}


import NodeTagsEdit._


trait NodeTagsEdit extends SioController with PlayMacroLogsI {
  
  /**
   * Добавление тега в редакторе тегов.
   * @param index Порядковый номер тега.
   */
  def tagEditorAddTag(index: Int) = IsAuth { implicit request =>
    val formBinded = tagAddFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        LOGGER.debug(s"tagEditorAddTag(): Form bind failed:\n ${formatFormErrors(formWithErrors)}")
        val formRendered = _addFormTpl(formWithErrors)
        NotAcceptable( formRendered )
      },
      {mtag =>
        val name = s"$AD_K.$TAGS_K[$index]"
        Ok( _oneTagTpl(formBinded, fprefix = Some(name)) )
      }
    )
  }

}
