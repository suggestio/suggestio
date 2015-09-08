package controllers.ad

import controllers.SioController
import io.suggest.common.tags.edit.TagsEditConstants
import models.MNodeTag
import play.api.data._
import util.PlayMacroLogsI
import util.acl.IsAuth
import views.html.lk.tag.edit._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:45
 * Description: Аддон для контроллера редактора карточки (узла), добавляющий экшены поддержки тегов.
 */
object NodeTagsEdit {

  /** Маппинг формы  */
  def tagAddFormM: Form[MNodeTag] = {
    val k = TagsEditConstants.ADD_NAME_INPUT_ID
    val m = MarketAdFormUtil.tagNameAsTagM
    Form(k -> m)
  }

}


import NodeTagsEdit._


trait NodeTagsEdit extends SioController with PlayMacroLogsI {
  
  /** Добавление тега в редакторе тегов. */
  def tagEditorAddTag = IsAuth { implicit request =>
    implicit val ctx = getContext2
    tagAddFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"tagEditorAddTag(): Form bind failed:\n ${formatFormErrors(formWithErrors)}")
        //val formRendered = _addFormTpl(formWithErrors, )
        //NotAcceptable(  )
        ???
      },
      {mtag =>
        ???
      }
    )
  }

}
