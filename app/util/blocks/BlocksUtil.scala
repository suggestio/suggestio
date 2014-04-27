package util.blocks

import play.api.data._, Forms._
import util.FormUtil._
import play.api.templates.{HtmlFormat, Template4}
import models.Context
import views.html.blocks.editor._
import BlocksConf.BlockConf

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 21:50
 * Description:
 */

object BlocksUtil {
  type BlockMap = Map[String, Any]

  // BK-константы именуют все используемые ключи конфигов. Полезно для избежания ошибок в разных местах.
  val BK_HEIGHT       = "height"
  val BK_TITLE        = "title"
  val BK_DESCRIPTION  = "description"
  val BK_PHOTO_ID     = "photoId"
  val BK_BG_COLOR     = "bgColor"

  val bTitleM = nonEmptyText(minLength = 2, maxLength = 250)
    .transform[String](strTrimSanitizeF, strIdentityF)

  def bDescriptionM = publishedTextM

}

import BlocksUtil._


object BlocksEditorFields extends Enumeration {

  protected abstract case class Val(name: String) extends super.Val(name) {
    def fieldTemplate: Template4[String, Option[Any], BlockConf, Context, HtmlFormat.Appendable]
    def render(key: String, value: Option[Any], bc: BlockConf)(implicit ctx: Context) = {
      fieldTemplate.render(key, value, bc, ctx)
    }
  }

  type BlockEditorField = Val

  implicit def value2val(x: Value): BlockEditorField = x.asInstanceOf[BlockEditorField]

  val Height = new Val("height") {
    def fieldTemplate = _heightTpl
  }

  val InputText = new Val("inputText") {
    def fieldTemplate = _inputTextTpl
  }

  val TextArea = new Val("textarea") {
    def fieldTemplate = _textareaTpl
  }

}

import BlocksEditorFields.BlockEditorField


case class BlockField(name: String, field: BlockEditorField)

