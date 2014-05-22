package util.blocks

import play.api.data._, Forms._
import util.FormUtil._
import play.api.templates.{Template5, HtmlFormat}
import models._
import views.html.blocks.editor._
import BlocksConf.BlockConf
import controllers.ad.MarketAdFormUtil
import io.suggest.ym.model.common.{IColors, IBlockMeta, BlockMeta}
import io.suggest.ym.model.ad.{AOValueField, IOffers}
import util.img._
import controllers.MarketAdPreview.PreviewFormDefaults
import io.suggest.img.SioImageUtilT
import util.img.ImgInfo4Save
import util.FormUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 21:50
 * Description:
 */

object BlocksUtil {

  type BlockImgMap = Map[String, ImgInfo4Save[ImgIdKey]]

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

  // Допустимые ширины блоков.
  val BLOCK_WIDTH_NORMAL_PX = 300
  val BLOCK_WIDTH_NARROW_PX = 140

  /** Линейка размеров шрифтов. */
  val FONT_SIZES_DFLT: List[FontSize] = List(
    FontSize(18, 12), FontSize(22, 16), FontSize(26, 18), FontSize(30, 20), FontSize(34, 22), FontSize(38, 24),
    FontSize(42, 28), FontSize(46, 30), FontSize(50, 34), FontSize(54, 36), FontSize(58, 38), FontSize(62, 40),
    FontSize(66, 42), FontSize(70, 44), FontSize(74, 46), FontSize(80, 48), FontSize(84, 50)
  )
}

import BlocksUtil._


object BlocksEditorFields extends Enumeration {

  // TODO Наверное надо параметризовать BFT или T, иначе тут какая-то задница с типами получается.
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
  
  protected trait HeightVal {
    type T = Int
    type BFT = BfHeight
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
    type T = BlockImgMap
    type BFT = BfImage
  }

  protected trait DiscountVal {
    type T = AOFloatField
    type BFT = BfDiscount
  }

  protected trait ColorVal {
    type T = String
    type BFT = BfColor
  }

  type BlockEditorField   = Val
  type BefHeight          = BlockEditorField with HeightVal
  type BefDiscount        = BlockEditorField with DiscountVal
  type BefPrice           = BlockEditorField with PriceVal
  type BefText            = BlockEditorField with TextVal
  type BefString          = BlockEditorField with StringVal
  type BefImage           = BlockEditorField with ImageVal
  type BefColor           = BlockEditorField with ColorVal

  implicit def value2val(x: Value): BlockEditorField = {
    x.asInstanceOf[BlockEditorField]
  }

  /** Скрытое поле для указания высоты блока. */
  val Height: BefHeight = new Val("height") with HeightVal {
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
  val Price: BefPrice = new Val("price") with PriceVal {
    override def fieldTemplate = _priceTpl
  }

  /** Поле с кнопкой для загрузки картинки. */
  val Image: BefImage = new Val("img") with ImageVal {
    override def fieldTemplate = _imageTpl
  }
  
  val Discount: BefDiscount = new Val("discount") with DiscountVal {
    override def fieldTemplate = _discountTpl
  }

  val Color: BefColor = new Val("color") with ColorVal {
    override def fieldTemplate = _colorTpl
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


trait BlockAOValueFieldT extends BlockFieldT {
  override type T <: AOValueField

  def withFontColor: Boolean
  def withFontSizes: List[FontSize]
  def withFontSize = !withFontSizes.isEmpty
  def fontSizeDflt: Option[Int]
  def fontForSize(sz: Int): Option[FontSize] = withFontSizes.find(_.size == sz)
  def lineHeightDflt: Option[Int] = fontSizeDflt.flatMap(fontForSize).map(_.lineHeight)
  def withFontFamily: Boolean
  def withTextAlign: Boolean
  def defaultFont: AOFieldFont = BlocksUtil.defaultFont
  def getFontMapping = MarketAdFormUtil.getFontM(
    withFontSizes = withFontSizes
  )

  def withCoords: Boolean
}


object BfHeight {
  val HEIGHT_DFLT = Some(300)
  val HEIGHTS_AVAILABLE = Set(300, 460, 620)
}

/** Поле для какой-то цифры. */
case class BfHeight(
  name: String,
  defaultValue: Option[Int] = BfHeight.HEIGHT_DFLT,
  availableVals: Set[Int] = BfHeight.HEIGHTS_AVAILABLE
) extends BlockFieldT {
  override type T = Int
  override def field = BlocksEditorFields.Height
  override def offerNopt: Option[Int] = None

  override def fallbackValue: T = 140

  override def mappingBase = number
    .verifying("error.invalid", { availableVals.contains(_) })

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


case class BfPrice(
  name: String,
  offerNopt: Option[Int] = None,
  defaultValue: Option[AOPriceField] = None,
  withFontColor: Boolean = true,
  withFontSizes: List[FontSize] = FONT_SIZES_DFLT,
  dfltFontSize: Option[Int] = None,
  fontSizeDflt: Option[Int] = None,
  withFontFamily: Boolean = true,
  withCoords: Boolean = true,
  withTextAlign: Boolean = false
) extends BlockAOValueFieldT {
  override type T = AOPriceField

  def maxStrlen = FormUtil.PRICE_M_MAX_STRLEN

  override def mappingBase: Mapping[T] = MarketAdFormUtil.aoPriceFieldM(getFontMapping, withCoords)

  override def field: BefPrice = BlocksEditorFields.Price

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

  override def getOptionalStrictMapping: Mapping[Option[T]] = MarketAdFormUtil.aoPriceOptM(getFontMapping, withCoords)
}


case class BfText(
  name: String,
  field: BefText = BlocksEditorFields.TextArea,
  offerNopt: Option[Int] = None,
  defaultValue: Option[AOStringField] = None,
  minLen: Int = 0,
  maxLen: Int = 16000,
  withFontColor: Boolean = true,
  withFontSizes: List[FontSize] = FONT_SIZES_DFLT,
  fontSizeDflt: Option[Int] = None,
  withFontFamily: Boolean = true,
  withCoords: Boolean = true,
  withTextAlign: Boolean = true
) extends BlockAOValueFieldT {
  override type T = AOStringField

  def strTransformF = strTrimSanitizeF

  override val mappingBase: Mapping[T] = {
    val m0 = text(minLength = minLen, maxLength = maxLen)
      .transform(replaceEOLwithBR andThen strTrimBrOnlyF,  replaceBRwithEOL)
    MarketAdFormUtil.aoStringFieldM(m0, getFontMapping, withCoords)
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
  withFontColor: Boolean = true,
  withFontFamily: Boolean = false,
  withCoords: Boolean = false,
  withTextAlign: Boolean = false,
  minLen: Int = 0,
  maxLen: Int = 16000
) extends BlockFieldT {
  def fallbackValue = "example"

  override type T = String
  def strTransformF = strTrimSanitizeF

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
  defaultValue: Option[BlockImgMap] = None,
  offerNopt: Option[Int] = None,
  saveWithThumb: Boolean = false
) extends BlockFieldT {
  override type T = BlockImgMap

  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  override def fallbackValue: T = {
    val oiik = OrigImgIdKey(PreviewFormDefaults.IMG_ID, OrigImgData("", None))
    val i4s = ImgInfo4Save(oiik, withThumb = saveWithThumb)
    Map(name -> i4s)
  }

  /** Маппинг для картинок, которые можно кадрировать. Есть ключ картинки и есть настройки кадрирования. */
  override def mappingBase: Mapping[T] = {
    ImgFormUtil.imgIdMarkedOptM(marker = marker)
      .transform[BlockImgMap] (
        { _.map { iik => ImgInfo4Save(iik, withThumb = saveWithThumb) }
           .fold[BlockImgMap] (Map.empty) { i4s => Map(name -> i4s) }
        },
        { _.get(name).map(_.iik) }
      )
  }


  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


object BfDiscount {
  val DFLT: Option[AOFloatField] = Some(AOFloatField(50F, defaultFont))
}

/** Поле для ввода скидки в процентах. Кто-то хочет положительную скидку задавать, кто-то отрицательную. */
case class BfDiscount(
  name: String,
  defaultValue: Option[AOFloatField] = BfDiscount.DFLT,
  offerNopt: Option[Int] = None,
  min: Float = -99F,
  max: Float = 100F,
  withFontColor: Boolean = true,
  withFontSizes: List[FontSize] = Nil,
  fontSizeDflt: Option[Int] = None,
  withFontFamily: Boolean = false,
  withCoords: Boolean = false,
  withTextAlign: Boolean = false
) extends BlockAOValueFieldT {
  override type T = AOFloatField
  val discoFloatM = getTolerantDiscountPercentM(
    min = min,
    max = max,
    dflt = defaultValue
      .map(_.value)
      .getOrElse(fallbackValue.value)
  )

  def maxStrlen: Int = FormUtil.PERCENT_M_CHARLEN_MAX

  override def field: BefDiscount = BlocksEditorFields.Discount

  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  override def fallbackValue: T = AOFloatField(0F, defaultFont)

  override def mappingBase: Mapping[T] = {
    val mapping0 = MarketAdFormUtil.aoFloatFieldM(discoFloatM, getFontMapping, withCoords)
    defaultOpt(mapping0, defaultValue)
  }

  override def getOptionalStrictMapping: Mapping[Option[T]] = {
    MarketAdFormUtil.aoFloatFieldOptM(discoFloatM, getFontMapping, withCoords)
  }

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


case class BfColor(
  name: String,
  defaultValue: Option[String] = None,
  fallbackValue: String = "444444",
  offerNopt: Option[Int] = None
) extends BlockFieldT {
  override type T = String

  override def mappingBase: Mapping[T] = defaultOpt(colorM, defaultValue)
  override def getOptionalStrictMapping: Mapping[Option[T]] = colorOptM

  override def field = BlocksEditorFields.Color

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): HtmlFormat.Appendable = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


/** Класс-реализация для быстрого создания BlockData. Используется вместо new BlockData{}. */
case class BlockDataImpl(
  blockMeta: BlockMeta,
  offers: List[AOBlock],
  colors: Map[String, String] = Map.empty
)
  extends IBlockMeta
  with IOffers
  with IColors

