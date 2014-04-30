package util.blocks

import play.api.data._, Forms._
import util.FormUtil._
import play.api.templates.{Template5, HtmlFormat}
import models._
import views.html.blocks.editor._
import BlocksConf.BlockConf
import controllers.ad.MarketAdFormUtil
import io.suggest.ym.model.common.{IBlockMeta, BlockMeta}
import io.suggest.ym.model.ad.IOffers
import util.img.{OrigImgIdKey, ImgIdKey, ImgFormUtil}
import controllers.MarketAdPreview.PreviewFormDefaults
import io.suggest.ym.model.common.EMImg.Imgs_t
import io.suggest.img.SioImageUtilT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 21:50
 * Description:
 */

object BlocksUtil {

  type BlockImgMap = Map[String, ImgIdKey]

  val BLOCK_ID_FN = "blockId"

  val I18N_PREFIX = "blocks.field."

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

  implicit def imgs2bim(imgs: Imgs_t): BlockImgMap = {
    imgs.mapValues { mii => OrigImgIdKey(mii.id, mii.meta) }
  }
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
 
  protected trait TextVal {
    type T = AOStringField
    type BFT = BfText
  }
  
  protected trait IntVal {
    type T = Int
    type BFT = BfInt
  }
  
  protected trait PriceVal {
    type T = AOPriceField
    type BFT = BfPrice
  }

  protected trait StringVal {
    type T = String
    type BFT = BfString
  }

  protected trait ImageVal {
    type T = ImgIdKey
    type BFT = BfImage
  }

  type BlockEditorField   = Val
  type BefInt             = BlockEditorField with IntVal
  type BefFloat           = BlockEditorField with PriceVal
  type BefText            = BlockEditorField with TextVal
  type BefString          = BlockEditorField with StringVal
  type BefImage           = BlockEditorField with ImageVal

  implicit def value2val(x: Value): BlockEditorField = {
    x.asInstanceOf[BlockEditorField]
  }

  /** Скрытое поле для указания высоты блока. */
  val Height: BefInt = new Val("height") with IntVal {
    override def fieldTemplate = _heightTpl
  }

  /** input text с указанием цвета. */
  val InputText: BefText = new Val("inputText") with TextVal {
    override def fieldTemplate = _inputTextTpl
  }

  /** Это когда много букв с указанием цвета. */
  val TextArea: BefText = new Val("textarea") with TextVal {
    override def fieldTemplate = _textareaTpl
  }

  /** input text для задания цены. */
  val Price: BefFloat = new Val("price") with PriceVal {
    override def fieldTemplate = _priceTpl
  }

  /** Поле с кнопкой для загрузки картинки. */
  val Image: BefImage = new Val("img") with ImageVal {
    override def fieldTemplate = _imageTpl
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

  def offerNopt: Option[Int]
  def offerN = offerNopt getOrElse 0
}


/** Поле для какой-то цифры. */
case class BfInt(
  name: String,
  field: BefInt = BlocksEditorFields.Height,
  offerNopt: Option[Int] = None,
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
  field: BefFloat = BlocksEditorFields.Price,
  offerNopt: Option[Int] = None,
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

  override def getOptionalStrictMapping: Mapping[Option[T]] = MarketAdFormUtil.mmaPriceOptM
}


case class BfText(
  name: String,
  field: BefText,
  offerNopt: Option[Int] = None,
  defaultValue: Option[AOStringField] = None,
  minLen: Int = 0,
  maxLen: Int = 16000
) extends BlockFieldT {
  override type T = AOStringField

  def strTransformF = strTrimSanitizeLowerF

  override val mappingBase: Mapping[T] = {
    val m0 = text(minLength = minLen, maxLength = maxLen)
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

  override def getOptionalStrictMapping: Mapping[Option[T]] = {
    super.getOptionalStrictMapping
      .transform[Option[T]]({_.filter(!_.value.isEmpty)}, identity)
  }
}


/** Поля для строки. */
case class BfString(
  name: String,
  field: BefString,
  offerNopt: Option[Int] = None,
  defaultValue: Option[String] = None,
  minLen: Int = 0,
  maxLen: Int = 16000
) extends BlockFieldT {
  def fallbackValue = "example"

  override type T = String
  def strTransformF = strTrimSanitizeLowerF

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }

  override def mappingBase: Mapping[T] = {
    nonEmptyText(minLength = minLen, maxLength = maxLen)
      .transform(strTransformF, strIdentityF)
  }

  override def getOptionalStrictMapping: Mapping[Option[T]] = {
    super.getOptionalStrictMapping
      .transform[Option[T]]({_.filter(!_.isEmpty)}, identity)
  }
}


case class BfImage(
  name: String,
  marker: String,
  imgUtil: SioImageUtilT,
  field: BefImage = Image,
  defaultValue: Option[ImgIdKey] = None,
  offerNopt: Option[Int] = None
) extends BlockFieldT {
  override type T = ImgIdKey

  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  override def fallbackValue: T = OrigImgIdKey(PreviewFormDefaults.IMG_ID)

  override def mappingBase: Mapping[T] = {
    ImgFormUtil.imgIdMarkedM("error.image.invalid", marker = marker)
  }

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


/** Класс-реализация для быстрого создания BlockData. Используется вместо new BlockData{}. */
case class BlockDataImpl(blockMeta: BlockMeta, offers: List[AOBlock])
  extends IBlockMeta
  with IOffers

