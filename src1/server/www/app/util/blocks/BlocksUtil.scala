package util.blocks

import models.blk.ed.{BimKey_t, BlockImgMap}
import play.api.data._
import Forms._
import enumeratum.values.ValueEnumEntry
import io.suggest.ad.blk._
import io.suggest.ad.blk.ent.{EntFont, TextEnt, ValueEnt}
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

