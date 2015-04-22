package util.blocks

import models.blk._
import models.im.MImg
import play.api.data._, Forms._
import util.FormUtil._
import models._
import views.html.blocks.editor._
import controllers.ad.MarketAdFormUtil
import io.suggest.ym.model.common.{IColors, IEMBlockMeta, BlockMeta}
import io.suggest.ym.model.ad.{AOValueField, IOffers}
import util.img._
import play.twirl.api.{Html, Template5}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 21:50
 * Description: Всякая утиль для блоков, в основном для редактора блоков.
 */

object BlocksUtil {

  type BlockImgEntry = (String, MImg)
  type BlockImgMap = Map[String, MImg]

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


/** Трейт для значений BlockEditorField. */
sealed trait BefValT { bv =>
  type VT
  type BFT <: BlockFieldT { type T = VT }
  def fieldTemplate: Template5[BFT, String, Form[_], BlockConf, Context, Html]
  def renderEditorField(bf: BFT, bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
    fieldTemplate.render(bf, bfNameBase, af, bc, ctx)
  }
}


object BlocksEditorFields extends Enumeration {

  // TODO Наверное надо параметризовать BFT или T, иначе тут какая-то задница с типами получается.
  protected abstract class Val(val name: String) extends super.Val(name) with BefValT

  type BlockEditorField = Val

  implicit def value2val(x: Value): BlockEditorField = {
    x.asInstanceOf[BlockEditorField]
  }

  /** Скрытое поле для указания высоты блока. */
  val Height = new Val("height") {
    override type VT = Int
    override type BFT = BfHeight
    override def fieldTemplate = _heightTpl
  }

  val Width = new Val("width") {
    override type VT = Int
    override type BFT = BfWidth
    override def fieldTemplate = _widthTpl
  } 
 
  /** Ввод голой строки. */
  val InputString = new Val("inputStr") {
    override type VT = String
    override type BFT = BfString
    def fieldTemplate = _inputStringTpl
  }

  /** Это когда много букв с указанием цвета. */
  val TextArea = new Val("textarea") {
    override type VT = AOStringField
    override type BFT = BfText
    override def fieldTemplate = _textareaTpl
  }

  /** Поле с кнопкой для загрузки картинки. */
  val Image = new Val("img") {
    override type VT = BlockImgMap
    override type BFT = BfImage
    override def fieldTemplate = _imageTpl
  }

  val Color = new Val("color") {
    override type VT = String
    override type BFT = BfColor
    override def fieldTemplate = _colorTpl
  }
  
  val Checkbox = new Val("checkbox") {
    override type VT = Boolean
    override type BFT = BfCheckbox
    override def fieldTemplate = _checkboxTpl
  }

  /** специальный тип, подходит для вставляния левых шаблонов в набор полей редактора. */
  val NoValue = new Val("noval") {
    override type VT = None.type
    override type BFT = BfNoValueT
    override def fieldTemplate = _addStringFieldBtnTpl
  }
}

import BlocksEditorFields._


/** Трейт для конкретного поля в рамках динамического маппинга поля. */
trait BlockFieldT { that =>
  type T
  def name: String
  def field: BlockEditorField { type VT = that.T }
  def defaultValue: Option[T]
  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  def fallbackValue: T
  def anyDefaultValue: T = defaultValue getOrElse fallbackValue

  def mappingBase: Mapping[T]

  def getStrictMapping: Mapping[T] = defaultOpt(mappingBase, defaultValue)
  def getStrictMappingKV = name -> getStrictMapping

  def getOptionalStrictMapping: Mapping[Option[T]] = optional(mappingBase)
  def getOptionalStrictMappingKV = name -> getOptionalStrictMapping

  def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html

  def offerNopt: Option[Int]
  def offerN = offerNopt getOrElse 0
}


trait BlockAOValueFieldT extends BlockFieldT {
  override type T <: AOValueField

  def withFontColor: Boolean
  def withFontSize = true
  def fontSizeDflt: Option[Int]
  def withFontFamily: Boolean
  def withTextAlign: Boolean
  def defaultFont: AOFieldFont = BlocksUtil.defaultFont
  def getFontMapping = MarketAdFormUtil.fontM

  def withCoords: Boolean
}


/** Хелпер для полей ширины и высоты. */
sealed trait IntBlockSizeBf extends BlockFieldT {
  override type T = Int
  override def offerNopt: Option[Int] = None
  def availableVals: Set[Int]

  override def mappingBase = number
    .verifying("error.invalid", availableVals.contains(_) )
}

// TODO Нужно зафиксировать значения высоты через Enumeration. Это избавит от проблем с расчетами стоимостей рекламных модулей.
object BfHeight {
  def HEIGHT_DFLT = BlockHeights.default.heightPx
  val SOME_HEIGHT_DFLT = Some(BlockHeights.default.heightPx)
  val HEIGHTS_AVAILABLE_DFLT = BlockHeights.values.map(_.heightPx)
}

/** Поле для какой-то цифры. */
case class BfHeight(
  name          : String,
  defaultValue  : Option[Int] = BfHeight.SOME_HEIGHT_DFLT,
  availableVals : Set[Int] = BfHeight.HEIGHTS_AVAILABLE_DFLT
) extends IntBlockSizeBf {
  override def field = BlocksEditorFields.Height
  override def fallbackValue: T = BlockHeights.H140.heightPx

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}

object BfWidth {
  val WIDTH_DFLT = Some( BlockWidths.default.widthPx )
  val WIDTHS_AVAILABLE_DFLT = BlockWidths.values.map(_.widthPx)
}
case class BfWidth(
  name          : String,
  defaultValue  : Option[Int] = BfWidth.WIDTH_DFLT,
  availableVals : Set[Int] = BfWidth.WIDTHS_AVAILABLE_DFLT
) extends IntBlockSizeBf {
  override def field = BlocksEditorFields.Width
  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
  override def fallbackValue = BlockWidths.default.widthPx
}


case class BfText(
  name            : String,
  offerNopt       : Option[Int] = None,
  defaultValue    : Option[AOStringField] = None,
  minLen          : Int = 0,
  maxLen          : Int = 512,
  withFontColor   : Boolean = true,
  fontSizeDflt    : Option[Int] = None,
  withFontFamily  : Boolean = true,
  withCoords      : Boolean = true,
  withTextAlign   : Boolean = true
) extends BlockAOValueFieldT {
  override type T = AOStringField

  def strTransformF = strTrimSanitizeF

  override def field = BlocksEditorFields.TextArea

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

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }

  override def getOptionalStrictMapping: Mapping[Option[T]] = {
    super.getOptionalStrictMapping
      .transform[Option[T]]({_.filter(!_.value.isEmpty)}, identity)
  }
}


/** Поля для строки. */
case class BfString(
  name            : String,
  offerNopt       : Option[Int] = None,
  defaultValue    : Option[String] = None,
  withFontColor   : Boolean = true,
  withFontFamily  : Boolean = false,
  withCoords      : Boolean = false,
  withTextAlign   : Boolean = false,
  minLen          : Int = 0,
  maxLen          : Int = 16000
) extends BlockFieldT {
  def fallbackValue = "example"

  override def field = InputString

  override type T = String
  def strTransformF = strTrimSanitizeF

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
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
  name                : String,
  marker              : String,
  defaultValue        : Option[BlockImgMap] = None,
  offerNopt           : Option[Int] = None,
  preDetectMainColor  : Boolean = false,
  preserveFmt         : Boolean = false
) extends BlockFieldT {
  override type T = BlockImgMap

  override def field = Image

  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  override def fallbackValue: T = {
    //val oiik = OrigImgIdKey(PreviewFormDefaults.IMG_ID, OrigImgData("", None))
    //val i4s = ImgInfo4Save(oiik, withThumb = saveWithThumb)
    //Map(name -> i4s)
    // TODO Нужно fallback-картинку запилить и чтобы на неё была ссылка? Возможно, этот метод никогда не вызывается.
    Map.empty
  }

  /** Маппинг для картинок, которые можно кадрировать. Есть ключ картинки и есть настройки кадрирования. */
  override def mappingBase: Mapping[T] = {
    ImgFormUtil.imgIdOptM
      .transform[BlockImgMap] (
        { _.fold[BlockImgMap] (Map.empty) { i4s => Map(name -> i4s) } },
        { _.get(name) }
      )
  }


  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
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

  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


case class BfCheckbox(
  name          : String,
  defaultValue  : Option[Boolean] = None,
  fallbackValue : Boolean = false,
  offerNopt     : Option[Int] = None
) extends BlockFieldT {
  override type T = Boolean
  override def field = Checkbox
  override def mappingBase: Mapping[T] = boolean
  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
}


/** Реализация полей-пустышек. */
trait BfNoValueT extends BlockFieldT {
  override type T = None.type
  override def field = NoValue
  override def mappingBase: Mapping[T] = optional(text).transform[T]({_ => None}, identity)
  override def renderEditorField(bfNameBase: String, af: Form[_], bc: BlockConf)(implicit ctx: Context): Html = {
    field.renderEditorField(this, bfNameBase, af, bc)
  }
  override def defaultValue: Option[T] = None
  override def fallbackValue: T = None
  override def offerNopt: Option[Int] = None
}


/** Поле, которое не требует никакого значения для себя. Бывает полезно, когда надо вставить что-нибудь между полями. */
case class BfAddStringField(name: String = "addTextField") extends BfNoValueT

/** Класс-реализация для быстрого создания BlockData. Используется вместо new BlockData{}. */
case class BlockDataImpl(
  blockMeta   : BlockMeta,
  offers      : List[AOBlock],
  colors      : Map[String, String] = Map.empty
)
  extends IEMBlockMeta
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
