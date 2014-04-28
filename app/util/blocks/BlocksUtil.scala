package util.blocks

import play.api.data._, Forms._
import util.FormUtil._
import play.api.templates.{HtmlFormat, Template4}
import models.Context
import views.html.blocks.editor._
import BlocksConf.BlockConf
import io.suggest.model.EsModel

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 21:50
 * Description:
 */

object BlocksUtil {
  type BlockMap = Map[String, Any]

  // BK-константы именуют все используемые ключи конфигов. Полезно для избежания ошибок в разных местах.
  val BK_BLOCK_ID     = "blockId"
  val BK_HEIGHT       = "height"
  val BK_TITLE        = "title"
  val BK_DESCRIPTION  = "description"
  val BK_PHOTO_ID     = "photoId"
  val BK_BG_COLOR     = "bgColor"

  val bTitleM = nonEmptyText(minLength = 2, maxLength = 250)
    .transform[String](strTrimSanitizeF, strIdentityF)

  def bDescriptionM = publishedTextM

  def extractBlockId(bm: BlockMap) = EsModel.intParser(bm(BK_BLOCK_ID))
}

import BlocksUtil._


object BlocksEditorFields extends Enumeration {

  protected abstract case class Val(name: String) extends super.Val(name) {
    type T
    type BFT <: BlockFieldT
    def fieldTemplate: Template4[BFT, Field, BlockConf, Context, HtmlFormat.Appendable]
    def renderEditorField(bf: BFT, formField: Field, bc: BlockConf)(implicit ctx: Context) = {
      fieldTemplate.render(bf, formField, bc, ctx)
    }
    def parseAnyVal(anyV: Any): Option[T]
  }
  
  protected trait StringVal {
    type T = String
    type BFT = StringBlockFieldT
    def parseAnyVal(anyV: Any): Option[T] = Some(anyV.toString)
  }
  
  protected trait IntVal {
    type T = Int
    type BFT = NumberBlockField
    def parseAnyVal(anyV: Any): Option[T] = Some(EsModel.intParser(anyV))
  }

  type BlockEditorField = Val
  type StringBlockEditorField = BlockEditorField with StringVal
  type NumberBlockEditorField = BlockEditorField with IntVal

  implicit def value2val(x: Value): BlockEditorField = {
    x.asInstanceOf[BlockEditorField]
  }


  val Height: NumberBlockEditorField = new Val("height") with IntVal {
    override def fieldTemplate = _heightTpl
  }

  val InputText: StringBlockEditorField = new Val("inputText") with StringVal {
    override def fieldTemplate = _inputTextTpl
  }

  val TextArea: StringBlockEditorField = new Val("textarea") with StringVal {
    override def fieldTemplate = _textareaTpl
  }

  val Color: StringBlockEditorField = new Val("color") with StringVal {
    override def fieldTemplate = _colorTpl
  }
}

import BlocksEditorFields._


trait BlockFieldT {
  type T
  def name: String
  def field: BlockEditorField
  def defaultValue: Option[T]

  def getMapping: Mapping[T]
  def renderEditorField(formField: Field, bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable
  def anyValueToT(v: Any): Option[T]
  def extractValue(bm: BlockMap): Option[T] = {
    bm.get(name)
      .flatMap(anyValueToT)
      .orElse(defaultValue)
  }
}


case class NumberBlockField(
  name: String,
  field: NumberBlockEditorField,
  defaultValue: Option[Int] = None,
  minValue: Int = Int.MinValue,
  maxValue: Int = Int.MaxValue
) extends BlockFieldT {
  override type T = Int

  override def getMapping: Mapping[T] = {
    val mapping0 = number(min = minValue, max = maxValue)
    if (defaultValue.isDefined)
      default(mapping0, defaultValue.get)
    else
      mapping0
  }

  override def renderEditorField(formField: Field, bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, formField, bc)
  }
  override def anyValueToT(v: Any): Option[T] = field.parseAnyVal(v)
}



trait StringBlockFieldT extends BlockFieldT {
  override type T = String
  override def field: StringBlockEditorField
  override def anyValueToT(v: Any): Option[T] = field.parseAnyVal(v)
  override def renderEditorField(formField: Field, bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, formField, bc)
  }
  def minLen: Int
  def maxLen: Int
}


case class StringBlockField(
  name: String,
  field: StringBlockEditorField,
  strTransformF: String => String = strTrimSanitizeLowerF,
  defaultValue: Option[String] = None,
  minLen: Int = 0,
  maxLen: Int = 16000
) extends StringBlockFieldT {
  override def getMapping: Mapping[T] = {
    val mapping0 = nonEmptyText(minLength = minLen, maxLength = maxLen)
      .transform(strTransformF, strIdentityF)
    if (defaultValue.isDefined)
      default(mapping0, defaultValue.get)
    else
      mapping0
  }
}


case class ColorBlockField(
  name: String,
  field: StringBlockEditorField,
  defaultValue: Option[String] = None
) extends StringBlockFieldT {
  override def getMapping: Mapping[T] = {
    if (defaultValue.isDefined)
      default(colorM, defaultValue.get)
    else
      colorM
  }
  override def maxLen: Int = 6
  override def minLen: Int = maxLen
}

