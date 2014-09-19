package util.blocks

import play.api.data._, Forms._
import util.FormUtil._
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
import play.twirl.api.{HtmlFormat, Template5}

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

  /** Цвет фона под картинкой, когда та ещё не загружена. */
  val IMG_BG_COLOR_FN = "ibgc"

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
  // TODO Нужно зафиксировать допустимые значения ширины через Enumeration. Это избавит от проблем с расчетами стоимостей рекламных модулей.
  val BLOCK_WIDTH_NORMAL_PX = 300
  val BLOCK_WIDTH_NARROW_PX = 140

  /** Линейка размеров шрифтов. */
  val FONT_SIZES_DFLT: List[FontSize] = List(
    FontSize(10, 8), FontSize(12, 10), FontSize(14, 12), FontSize(16, 14),
    FontSize(18, 16), FontSize(22, 20), FontSize(26, 24), FontSize(30, 28), FontSize(34, 30), FontSize(38, 34),
    FontSize(42, 38), FontSize(46, 42), FontSize(50, 46), FontSize(54, 50), FontSize(58, 54), FontSize(62, 58),
    FontSize(66, 62), FontSize(70, 66), FontSize(74, 70), FontSize(80, 76), FontSize(84, 80)
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

  val InputString: BefString = new Val("inputStr") with StringVal {
    def fieldTemplate = _inputStringTpl
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
  def withFontSize = withFontSizes.nonEmpty
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


// TODO Нужно зафиксировать значения высоты через Enumeration. Это избавит от проблем с расчетами стоимостей рекламных модулей.
object BfHeight {
  val HEIGHT_DFLT = Some(300)

  val HEIGHT_140 = 140
  val HEIGHT_300 = 300
  val HEIGHT_460 = 460
  val HEIGHT_620 = 620

  val HEIGHTS_AVAILABLE_DFLT = Set(HEIGHT_300, HEIGHT_460, HEIGHT_620)
}

/** Поле для какой-то цифры. */
case class BfHeight(
  name: String,
  defaultValue: Option[Int] = BfHeight.HEIGHT_DFLT,
  availableVals: Set[Int] = BfHeight.HEIGHTS_AVAILABLE_DFLT
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
      .transform(replaceEOLwithBR andThen strTrimBrOnlyF,  replaceBRwithEOL andThen strUnescapeF)
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
  preserveFmt: Boolean = false,
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


/** Добавлялка длинного статического метода, который занимается объединением Either-результата
  * промежуточного биндинга и общего аккамулятора биндинга. */
trait MergeBindAcc[T] {
  /** Как-то обновить акк. */
  def updateAcc(offerN: Int, acc0: BindAcc, v: T)

  /**
   * Создать новый bind-аккамулятор (Either) на основе текущего bind-аккамулятора и результата
   * бинда текущего шага.
   * @param maybeAcc Either исходного аккамулятора с предыдущих шагов.
   * @param maybeV Either результата текущего биндинга.
   * @param offerN Необязательный номер оффера (блока).
   *               Используется в ListBlock-биндерах, в обычных биндерах всегда 0.
   *@return Новый Either-аккамулятор на основе объединения двух первых аргументов.
   */
  def mergeBindAcc(maybeAcc: Either[Seq[FormError], BindAcc],
                   maybeV: Either[Seq[FormError], T],
                   offerN: Int = 0): Either[Seq[FormError], BindAcc] = {
    (maybeAcc, maybeV) match {
      case (Right(acc0), Right(v)) =>
        updateAcc(offerN, acc0, v)
        maybeAcc

      case (Left(_), Right(_)) =>
        maybeAcc

      case (Right(_), Left(fes)) =>
        Left(fes)   // Избыточна пересборка left either из-за right-типа. Можно также вернуть через .asInstanceOf, но это плохо.

      case (Left(accFE), Left(fes)) =>
        Left(accFE ++ fes)
    }
  }

}

trait MergeBindAccAOBlock[T] extends MergeBindAcc[Option[T]] {

  /** Обновить указанный изменяемый AOBlock с помощью текущего значения. */
  def updateAOBlockWith(blk: AOBlock, v: Option[T])

  def updateAcc(offerN: Int, acc0: BindAcc, vOpt: Option[T]) {
    if (vOpt.isDefined) {
      acc0.offers.find { _.n == offerN } match {
        case Some(blk) =>
          updateAOBlockWith(blk, vOpt)
        case None =>
          val blk = AOBlock(n = offerN)
          updateAOBlockWith(blk, vOpt)
          acc0.offers ::= blk
      }
    }
  }

}
