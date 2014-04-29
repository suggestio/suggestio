package util.blocks

import play.api.data._, Forms._
import util.FormUtil._
import play.api.templates.{Template5, HtmlFormat, Template4}
import models._
import views.html.blocks.editor._
import BlocksConf.BlockConf
import io.suggest.model.EsModel
import controllers.ad.MarketAdFormUtil
import io.suggest.ym.model.common.{IBlockMeta, BlockMeta}
import io.suggest.ym.model.ad.IOffers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 21:50
 * Description:
 */

object BlocksUtil {

  val BLOCK_ID_FN = "blockId"


  val bTitleM = nonEmptyText(minLength = 2, maxLength = 250)
    .transform[String](strTrimSanitizeF, strIdentityF)

  def bDescriptionM = publishedTextM

  def extractBlockId(bd: BlockData) = bd.blockMeta.blockId

  def defaultOpt[T](m0: Mapping[T], defaultOpt: Option[T]): Mapping[T] = {
    if (defaultOpt.isDefined)
      default(m0, defaultOpt.get)
    else
      m0
  }

  def defaultFont: AOFieldFont = AOFieldFont(color = "000000")
}

import BlocksUtil._


object BlocksEditorFields extends Enumeration {

  protected abstract case class Val(name: String) extends super.Val(name) {
    type T
    type BFT <: BlockFieldT
    def fieldTemplate: Template5[BFT, String, Form[_], BlockConf, Context, HtmlFormat.Appendable]
    def renderEditorField(bf: BFT, bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
      fieldTemplate.render(bf, bfNameBase, af, bc, ctx)
    }
  }
 
  protected trait StringVal {
    type T = AOStringField
    type BFT = BfString
  }
  
  protected trait IntVal {
    type T = Int
    type BFT = BfInt
  }
  
  protected trait PriceVal {
    type T = AOPriceField
    type BFT = BfPrice
  }

  type BlockEditorField   = Val
  type BEFInt             = BlockEditorField with IntVal
  type BEFFloat           = BlockEditorField with PriceVal
  type BEFString          = BlockEditorField with StringVal

  implicit def value2val(x: Value): BlockEditorField = {
    x.asInstanceOf[BlockEditorField]
  }


  val Height: BEFInt = new Val("height") with IntVal {
    override def fieldTemplate = _heightTpl
  }

  val InputText: BEFString = new Val("inputText") with StringVal {
    override def fieldTemplate = _inputTextTpl
  }

  val TextArea: BEFString = new Val("textarea") with StringVal {
    override def fieldTemplate = _textareaTpl
  }

  val Price: BEFFloat = new Val("price") with PriceVal {
    override def fieldTemplate = _priceTpl
  }
}

import BlocksEditorFields._


/** Трейт для конкретного поля в рамках динамического маппинга поля. */
trait BlockFieldT {
  type T
  def name: String
  def field: BlockEditorField
  def defaultValue: Option[T]
  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  def fallbackValue: T
  def anyDefaultValue: T = defaultValue getOrElse fallbackValue

  def mappingBase: Mapping[T]

  def getStrictMapping: Mapping[T] = defaultOpt(mappingBase, defaultValue)
  def getStrictMappingKV = name -> getStrictMapping

  def getOptionalStrictMapping: Mapping[Option[T]] = optional(mappingBase)
  def getOptionalStrictMappingKV = name -> getOptionalStrictMapping

  def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable
}


/** Поле для какой-то цифры. */
case class BfInt(
  name: String,
  field: BEFInt = BlocksEditorFields.Height,
  defaultValue: Option[Int] = None,
  minValue: Int = Int.MinValue,
  maxValue: Int = Int.MaxValue
) extends BlockFieldT {
  override type T = Int

  override def fallbackValue: T = 140

  override val mappingBase = number(min = minValue, max = maxValue)

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


case class BfPrice(
  name: String,
  field: BEFFloat = BlocksEditorFields.Price,
  defaultValue: Option[AOPriceField] = None
) extends BlockFieldT {
  override type T = AOPriceField

  override def mappingBase: Mapping[T] = MarketAdFormUtil.mmaPriceM

  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  override def fallbackValue: T = AOPriceField(
    value = 100F,
    currencyCode = "RUB",
    orig = "100 рублей",
    font = defaultFont
  )

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


case class BfString(
  name: String,
  field: BEFString,
  strTransformF: String => String = strTrimSanitizeLowerF,
  defaultValue: Option[AOStringField] = None,
  minLen: Int = 0,
  maxLen: Int = 16000
) extends BlockFieldT {
  override type T = AOStringField

  override val mappingBase: Mapping[T] = {
    val m0 = nonEmptyText(minLength = minLen, maxLength = maxLen)
      .transform(strTrimSanitizeLowerF, strIdentityF)
    MarketAdFormUtil.mmaStringFieldM(m0)
  }

  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  override def fallbackValue: T = AOStringField(
    value = "Домик на рублёвке",    // TODO Нужен каталог примеров fallback-строк, новая на каждый раз.
    font = defaultFont
  )

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


/** Класс-реализация для быстрого создания BlockData. Используется вместо new BlockData{}. */
case class BlockDataImpl(blockMeta: BlockMeta, offers: List[AOBlock]) extends IBlockMeta with IOffers

