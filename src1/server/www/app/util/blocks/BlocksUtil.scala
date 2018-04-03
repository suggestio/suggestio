package util.blocks

import models.blk.ed.{BimKey_t, BindAcc, BlockImgMap}
import play.api.data._
import Forms._
import enumeratum.values.ValueEnumEntry
import io.suggest.ad.blk._
import io.suggest.ad.blk.ent.{EntFont, MEntity, TextEnt, ValueEnt}
import util.FormUtil._
import util.img.ImgFormUtil
import util.ad.LkAdEdFormUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 21:50
 * Description: Всякая утиль для блоков, в основном для редактора блоков.
 */

object BlocksUtil {

  private def inj = play.api.Play.current.injector
  private[blocks] val imgFormUtil       = inj.instanceOf[ImgFormUtil]
  private[blocks] val lkAdEdFormUtil    = inj.instanceOf[LkAdEdFormUtil]

  def defaultOpt[T](m0: Mapping[T], defaultOpt: Option[T]): Mapping[T] = {
    if (defaultOpt.isDefined)
      default(m0, defaultOpt.get)
    else
      m0
  }

  def defaultFont: EntFont = EntFont(color = "000000")

}


import BlocksUtil._


/** Трейт для конкретного поля в рамках динамического маппинга поля. */
trait BlockFieldT { that =>
  type T
  def name: String
  def defaultValue: Option[T]
  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  def fallbackValue: T
  def anyDefaultValue: T = defaultValue getOrElse fallbackValue

  def mappingBase: Mapping[T]

  def getStrictMapping: Mapping[T] = defaultOpt(mappingBase, defaultValue)

  def getOptionalStrictMapping: Mapping[Option[T]] = optional(mappingBase)
  def getOptionalStrictMappingKV = name -> getOptionalStrictMapping

  def offerNopt: Option[Int]
  def offerN = offerNopt getOrElse 0
  def withCoords: Boolean = false
}


trait BlockAOValueFieldT extends BlockFieldT {
  override type T <: ValueEnt

  def withFontColor: Boolean
  def withFontSize = true
  def fontSizeDflt: Option[Int]
  def withFontFamily: Boolean
  def withTextAlign: Boolean
  def defaultFont: EntFont = BlocksUtil.defaultFont
  def getFontMapping = lkAdEdFormUtil.fontM

  //def withCoords: Boolean
}


/** Хелпер для полей ширины и высоты. */
sealed trait IntValueEnumBlockSizeBf extends BlockFieldT {
  override type T <: ValueEnumEntry[Int]
  override def offerNopt: Option[Int] = None

  def availableVals: Seq[T]
  def availableValsMin = availableVals.head
  def availableValsMax = availableVals.last

}

// TODO Нужно зафиксировать значения высоты через Enumeration. Это избавит от проблем с расчетами стоимостей рекламных модулей.
object BfHeight {
  def HEIGHT_DFLT = BlockHeights.default
  val SOME_HEIGHT_DFLT = Some(BlockHeights.default)
  //val HEIGHTS_AVAILABLE_DFLT = BlockHeights.values
}


/** Поле для какой-то цифры. */
case class BfHeight(
  override val name             : String,
  override val defaultValue     : Option[BlockHeight]  = BfHeight.SOME_HEIGHT_DFLT
)
  extends IntValueEnumBlockSizeBf
{

  override type T = BlockHeight
  override def availableVals = BlockHeights.values

  override def fallbackValue: T = availableValsMin

  override def mappingBase = BlockMetaJvm.blockHeightMapping
  override def offerNopt = None

}


object BfWidth {
  val WIDTH_DFLT = Some( BlockWidths.default )
}
case class BfWidth(
  override val name          : String,
  override val defaultValue  : Option[BlockWidth] = BfWidth.WIDTH_DFLT
) extends IntValueEnumBlockSizeBf {

  override type T = BlockWidth

  override def mappingBase = BlockMetaJvm.blockWidthMapping
  override def offerNopt = None
  override def availableVals = BlockWidths.values
  override def fallbackValue = BlockWidths.default
}


case class BfText(
  override val name             : String,
  override val offerNopt        : Option[Int]     = None,
  override val defaultValue     : Option[TextEnt] = None,
  minLen                        : Int             = 0,
  maxLen                        : Int             = 512,
  withFontColor                 : Boolean         = true,
  fontSizeDflt                  : Option[Int]     = None,
  withFontFamily                : Boolean         = true,
  override val withCoords       : Boolean         = true,
  withTextAlign                 : Boolean         = true
)
  extends BlockAOValueFieldT
{
  override type T = TextEnt

  def strTransformF = strTrimSanitizeF

  override val mappingBase: Mapping[T] = {
    val m0 = text(minLength = minLen, maxLength = maxLen)
      .transform(replaceEOLwithBR andThen strTrimBrOnlyF,  replaceBRwithEOL andThen strUnescapeF)
    lkAdEdFormUtil.aoStringFieldM(m0, getFontMapping)
  }

  /** Когда очень нужно получить от поля какое-то значение, можно использовать fallback. */
  override def fallbackValue: T = TextEnt(
    value = "Example text",    // TODO Нужен каталог примеров fallback-строк, локализованных, новая на каждый раз.
    font = defaultFont
  )


  override def getOptionalStrictMapping: Mapping[Option[T]] = {
    super.getOptionalStrictMapping
      .transform[Option[T]]({_.filter(!_.value.isEmpty)}, identity)
  }
}


/** Поля для строки. */
case class BfString(
  override val name             : String,
  override val offerNopt        : Option[Int] = None,
  override val defaultValue     : Option[String] = None,
  withFontColor                 : Boolean = true,
  withFontFamily                : Boolean = false,
  override val withCoords       : Boolean = false,
  withTextAlign                 : Boolean = false,
  minLen                        : Int = 0,
  maxLen                        : Int = 16000
)
  extends BlockFieldT
{
  def fallbackValue = "example"

  override type T = String
  def strTransformF = strTrimSanitizeF

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
  override val name             : String,
  marker                        : String,
  bimKey                        : BimKey_t,
  override val defaultValue     : Option[BlockImgMap] = None,
  override val offerNopt        : Option[Int] = None,
  preDetectMainColor            : Boolean = false,
  preserveFmt                   : Boolean = false
)
  extends BlockFieldT
{
  override type T = BlockImgMap

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
    BlocksUtil.imgFormUtil.img3IdOptM
      .transform[BlockImgMap] (
        { _.fold[BlockImgMap] (Map.empty) { i4s =>
          Map(bimKey -> i4s)
        } },
        { _.get(bimKey) }
      )
  }

}


case class BfColor(
  override val name           : String,
  override val defaultValue   : Option[String]  = None,
  override val fallbackValue  : String          = "444444",
  override val offerNopt      : Option[Int]     = None
)
  extends BlockFieldT
{
  override type T = String

  override def mappingBase: Mapping[T] = defaultOpt(colorM, defaultValue)
  override def getOptionalStrictMapping: Mapping[Option[T]] = colorOptM


}


case class BfCheckbox(
  override val name          : String,
  override val defaultValue  : Option[Boolean]  = None,
  override val fallbackValue : Boolean          = false,
  override val offerNopt     : Option[Int]      = None
)
  extends BlockFieldT
{
  override type T = Boolean
  override def mappingBase: Mapping[T] = boolean
}


/** Реализация полей-пустышек. */
trait BfNoValueT extends BlockFieldT {
  override type T = None.type
  override def mappingBase: Mapping[T] = {
    optional(text)
      .transform[T]({_ => None}, identity)
  }
  override def defaultValue: Option[T] = None
  override def fallbackValue: T = None
  override def offerNopt: Option[Int] = None
}


/** Поле, которое не требует никакого значения для себя. Бывает полезно, когда надо вставить что-нибудь между полями. */
case class BfAddStringField(name: String = "addTextField") extends BfNoValueT


/** Добавлялка длинного статического метода, который занимается объединением Either-результата
  * промежуточного биндинга и общего аккамулятора биндинга. */
trait MergeBindAcc[T] {

  /** Как-то обновить акк. */
  def updateAcc(offerN: Int, acc0: BindAcc, v: T): BindAcc

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
        val acc1= updateAcc(offerN, acc0, v)
        Right(acc1)

      case (Left(_), Right(_)) =>
        maybeAcc

      case (Right(_), Left(fes)) =>
        Left(fes)   // Избыточна пересборка left either из-за right-типа. Можно также вернуть через .asInstanceOf, но это плохо.

      case (Left(accFE), Left(fes)) =>
        Left(accFE ++ fes)
    }
  }

}

trait MergeBindAccEntity[T] extends MergeBindAcc[Option[T]] {

  /** Обновить указанный изменяемый AOBlock с помощью текущего значения. */
  def updateEntityWith(blk: MEntity, v: Option[T]): MEntity

  def updateAcc(offerN: Int, acc0: BindAcc, vOpt: Option[T]): BindAcc = {
    if (vOpt.isDefined) {
      val offers1 = {
        val (found, rest) = acc0.offers
          .partition { _.id == offerN }
        val off00 = found.headOption
        val off0 = off00 getOrElse MEntity(offerN, text = None, coords = None)
        val off1 = updateEntityWith(off0, vOpt)
        off1 :: rest
      }
      acc0.copy(
        offers = offers1
      )

    } else {
      acc0
    }
  }

}
