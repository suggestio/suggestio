package controllers.ad

import models._
import models.blk.{FontSizes, AdColorFns, FontSize}
import util.FormUtil._
import play.api.data._, Forms._
import util.blocks.BlockMapperResult
import io.suggest.ym.model.ad.RichDescr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:15
 * Description: Общая утиль для работы с разными ad-формами: preview и обычными.
 */
object MarketAdFormUtil {

  type FormDetected_t = Option[(AdOfferType, AdFormM)]


  /** Маппинг для выравнивания текста в рамках поля. */
  def textAlignOptM: Mapping[Option[TextAlign]] = {
    optional(text(maxLength = 10))
      .transform[Option[TextAlign]](
        {_.filter(_.length <= 10)
          .flatMap { s =>
            if (s.length == 1) {
              TextAligns.maybeWithName(s)
            } else if (s.length == 0) {
              None
            } else {
              TextAligns.maybeWithCssName(s)
            }
          }
        },
        { _.map(_.cssName) }
      )
  }


  /** Маппинг для размера шрифта. */
  def fontSizeM: Mapping[FontSize] = {
    number(min = FontSizes.min.size, max = FontSizes.max.size)
      .transform [Option[FontSize]] (FontSizes.maybeWithSize, _.getOrElse(FontSizes.min).size)
      .verifying("error.unavailable.font.size", _.isDefined)
      .transform[FontSize](_.get, Some.apply)
  }

  /** Маппер для значения font.family. */
  def fontFamilyOptM: Mapping[Option[String]] = {
    optional(
      text(maxLength = 32)
        .verifying("error.font.unknown", {s => blk.Fonts.values.exists(_.fileName == s)} )
    )
  }


  /** Маппер для описания, прилагаемого к рекламной карточке. */
  def richDescrOptM: Mapping[Option[RichDescr]] = {
    val rdTextM = text(maxLength = 8192)
      .transform(strFmtTrimF, strIdentityF)
    val m = mapping(
      "bgColor" -> colorM,
      "text"    -> rdTextM
    )
    {(bgColor, rdText) =>
      if (rdText.isEmpty) {
        None
      } else {
        Some(RichDescr(bgColor = bgColor, text = rdText))
      }
    }
    {rdOpt =>
      val bgColor = rdOpt.fold("FFFFFF")(_.bgColor)
      val rdText  = rdOpt.fold("")(_.text)
      Some( (bgColor, rdText) )
    }
    optional(m)
      .transform(_.flatten, Option.apply)
  }


  /**
   * Сборка маппинга для шрифта.
   * @return Маппинг для AOFieldFont.
   */
  def fontM: Mapping[AOFieldFont] = {
    mapping(
      "color"  -> colorM,
      "size"   -> fontSizeM,
      "align"  -> textAlignOptM,
      "family" -> fontFamilyOptM
    )
    {(color, fsz, align, family) =>
      AOFieldFont(
        color  = color,
        size   = Some(fsz.size),
        align  = align,
        family = family
      )
    }
    {aoff =>
      val fsz: FontSize = aoff.size
        .flatMap { FontSizes.maybeWithSize }
        .getOrElse { FontSizes.min }
      import aoff._
      Some((color, fsz, align, family))
    }
  }


  /** Маппер для активации и настройки покрывающей сетки-паттерна указанного цвета. */
  def coveringPatternM: Mapping[Option[String]] = {
    tuple(
      "enabled" -> boolean,
      "color"   -> optional(colorM)
    )
    .verifying("error.required", {m => m match {
      case (true, None)   => false
      case _              => true
    }})
    .transform[Option[String]] (
      { case (isEnabled, colorOpt) => colorOpt.filter(_ => isEnabled) },
      { colorOpt => (colorOpt.isDefined, colorOpt) }
    )
  }


  /** Парсер координаты. Координата может приходить нецелой, поэтому нужно округлить. */
  def coordM: Mapping[Int] = {
    // TODO Достаточно парсить первые цифры до $ или до десятичной точки/запятой, остальное отбрасывать.
    doubleM
      .transform[Int](_.toInt, _.toDouble)
      .verifying("error.coord.too.big", { _ <= 2048 })
      .transform[Int](Math.max(0, _), identity)
  }
  def coords2DM: Mapping[Coords2D] = {
    // сохраняем маппинг в переменную на случай если coordM станет def вместо val.
    val _coordM = coordM
    mapping(
      "x" -> _coordM,
      "y" -> _coordM
    )
    { Coords2D.apply }
    { Coords2D.unapply }
  }
  val coords2DOptM: Mapping[Option[Coords2D]] = optional(coords2DM)


  /** Маппим строковое поле с настройками шрифта. */
  def aoStringFieldM(m: Mapping[String], fontM: Mapping[AOFieldFont], withCoords: Boolean): Mapping[AOStringField] = {
    if (withCoords) {
      mapping(
        "value" -> m,
        "font"  -> fontM,
        "coords" -> coords2DOptM
      )
      { AOStringField.apply }
      { AOStringField.unapply }
    } else {
      mapping(
        "value" -> m,
        "font"  -> fontM
      )
      { AOStringField.apply(_, _) }
      { aosf => AOStringField.unapply(aosf).map { u => u._1 -> u._2 } }
    }
  }

  /** Маппим числовое (Float) поле. */
  def aoFloatFieldM(m: Mapping[Float], fontM: Mapping[AOFieldFont], withCoords: Boolean): Mapping[AOFloatField] = {
    if (withCoords) {
      mapping(
        "value"  -> m,
        "font"   -> fontM,
        "coords" -> coords2DOptM
      )
      { AOFloatField.apply }
      { AOFloatField.unapply }
    } else {
      mapping(
        "value" -> m,
        "font"  -> fontM
      )
      { AOFloatField.apply(_, _) }
      { AOFloatField.unapply(_).map { u => u._1 -> u._2} }
    }
  }


  /** Маппим необязательное Float-поле. */
  def aoFloatFieldOptM(m: Mapping[Float], fontM: Mapping[AOFieldFont], withCoords: Boolean): Mapping[Option[AOFloatField]] = {
    if (withCoords) {
      mapping(
        "value"  -> optional(m),
        "font"   -> fontM,
        "coords" -> coords2DOptM
      )
      {(valueOpt, color, coordsOpt) =>
        valueOpt map { AOFloatField(_, color, coordsOpt) }
      }
      {_.map { mmaff =>
        (Option(mmaff.value), mmaff.font, mmaff.coords)
      }}
    } else {
      mapping(
        "value" -> optional(m),
        "font"  -> fontM
      )
      {(valueOpt, color) =>
        valueOpt map { AOFloatField(_, color) }
      }
      {_.map { mmaff =>
        (Option(mmaff.value), mmaff.font)
      }}
    }
  }


  /** apply-функция для формы добавления/редактировать рекламной карточки.
    * Вынесена за пределы генератора ad-маппингов во избежание многократного создания в памяти экземпляров функции. */
  def adFormApply(userCatId: Set[String], bmr: BlockMapperResult, pattern: Option[String],
                  richDescrOpt: Option[RichDescr], bgColor: String): AdFormMResult = {
    val colors: Map[String, String] = {
      // Чтобы немного сэкономить ресурсов на добавлении цветов, используем склеивание итераторов и генерацию финальной мапы.
      var ci = bmr.bd.colors.iterator
      ci ++= Iterator(AdColorFns.IMG_BG_COLOR_FN.name -> bgColor)
      if (pattern.isDefined)
        ci ++= Iterator(AdColorFns.WIDE_IMG_PATTERN_COLOR_FN.name -> pattern.get)
      ci.toMap
    }
    val mad = MAd(
      producerId  = null,
      offers      = bmr.bd.offers,
      blockMeta   = bmr.bd.blockMeta,
      colors      = colors,
      imgs        = null,
      userCatId   = userCatId,
      richDescrOpt = richDescrOpt
    )
    mad -> bmr.bim
  }

  /** Функция разборки для маппинга формы добавления/редактирования рекламной карточки. */
  def adFormUnapply(applied: AdFormMResult): Option[(Set[String], BlockMapperResult, Option[String], Option[RichDescr], String)] = {
    val mad = applied._1
    val bmr = BlockMapperResult(mad, applied._2)
    val pattern = mad.colors.get(AdColorFns.WIDE_IMG_PATTERN_COLOR_FN.name)
    import AdColorFns._
    val bgColor = mad.colors.getOrElse(IMG_BG_COLOR_FN.name, IMG_BG_COLOR_FN.default)
    Some( (mad.userCatId, bmr, pattern, mad.richDescrOpt, bgColor) )
  }

  val AD_K      = "ad"
  val CAT_ID_K  = "catId"
  val OFFER_K   = "offer"

  /**
   * Сборщик форм произвольного назначения для парсинга реквестов с данными рекламной карточки.
   * @param catIdM маппер для id категории.
   * @param blockM маппер для блоков.
   * @return Маппинг формы, готовый к эксплуатации.
   */
  def getAdFormM(catIdM: Mapping[Set[String]], blockM: Mapping[BlockMapperResult]): AdFormM = {
    Form(
      AD_K -> mapping(
        CAT_ID_K    -> catIdM,
        OFFER_K     -> blockM,
        "pattern"   -> coveringPatternM,
        "descr"     -> richDescrOptM,
        "bgColor"   -> colorM
      )(adFormApply)(adFormUnapply)
    )
  }



  /**
   * Определить владельца категорий узла.
   * @param adnNode Узел рекламной сети.
   * @return id узла-владельца категорий.
   */
  def getCatOwnerId(adnNode: MAdnNode): String = {
    /*
    import AdNetMemberTypes._
    adnNode.adn.memberType match {
      case SHOP | RESTAURANT => adnNode.adn.supId getOrElse adnNode.id.get
      case MART | RESTAURANT_SUP => adnNode.id.get
    }
    */
    MMartCategory.DEFAULT_OWNER_ID
  }

}

