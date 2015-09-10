package io.suggest.lk.tags.edit.m.madd

import scala.scalajs.js.{JSON, Dictionary}
import io.suggest.common.tags.edit.TagsEditConstants.ReplyOk._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 16:45
 * Description: Тег добавлен в список существующих. Сервер вернул новый рендер.
 */

trait IUpdateExisting extends IAddFormHtml {
  def existingHtml: String
}


case class UpdateExisting(json: String) extends IUpdateExisting {

  val dic: Dictionary[String] = {
    JSON.parse(json)
      .asInstanceOf[ Dictionary[String] ]
  }

  override def existingHtml = dic(EXIST_TAGS_FN)
  override def addFormHtml  = dic(ADD_FORM_FN)
}
