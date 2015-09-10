package controllers.ad

import models._
import models.blk._
import util.FormUtil._
import play.api.data._, Forms._
import util.blocks.BlockMapperResult
import io.suggest.ym.model.ad.RichDescr
import io.suggest.ad.form.AdFormConstants._

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


  private def _fontFamilyOptM: Mapping[Option[Font]] = {
    text(maxLength = 32)
      .transform [Option[Font]] (
        Fonts.maybeWithName,
        _.fold("")(_.fileName)
      )
  }

  /** Маппер для значения font.family. */
  def fontFamilyOptM: Mapping[Option[Font]] = {
    optional( _fontFamilyOptM )
      .transform[Option[Font]](_.flatten, Some.apply )
  }

  def fontFamily: Mapping[Font] = {
    _fontFamilyOptM
      .verifying("error.font.unknown", _.nonEmpty)
      .transform(_.get, Some.apply)
  }


  /** Маппер для описания, прилагаемого к рекламной карточке. */
  def richDescrOptM: Mapping[Option[RichDescr]] = {
    val rdTextM = text(maxLength = 20000)
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
        family = family.map(_.fileName)
      )
    }
    {aoff =>
      val fsz: FontSize = aoff.size
        .flatMap { FontSizes.maybeWithSize }
        .getOrElse { FontSizes.min }
      import aoff._
      val fontOpt = family flatMap { Fonts.maybeWithName }
      Some((color, fsz, align, fontOpt))
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
  def coords2DOptM: Mapping[Option[Coords2D]] = optional(coords2DM)


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

  /** Максимальная символьная длина одного тега. */
  def TAG_LEN_MAX = 40
  /** Минимальная символьная длина одного тега. */
  def TAG_LEN_MIN = 1

  /** причёсывание имени тега. */
  private def _tagNamePrepare(raw: String): String = {
    // Регистр сохраняем исходный. Это нужно для FTS-токенизации в случае склееных слов.
    stripHtml(raw)
      .replaceAll("(?U)([^\\w\\s]|_)", " ")
      // Убрать двойные пробелы, табуляции и т.д.
      .replaceAll("\\s+", " ")
      .trim()
  }

  private def _tagName2tag(tname: String): MNodeTag = {
    MNodeTag(
      id  = tname.toLowerCase,
      raw = tname
    )
  }

  private def _tagNamePrepareM(m0: Mapping[String]): Mapping[String] = {
    m0.transform[String](
      // Срезать знаки препинания, все \s+ заменить на одиночные пробелы.
      _tagNamePrepare, strIdentityF
    )
    .verifying("e.tag.len.max", _.length <= TAG_LEN_MAX)
    .verifying("e.tag.len.min", _.length >= TAG_LEN_MIN)
  }

  def tagNameM: Mapping[String] = {
    val m0 = nonEmptyText(
      minLength = TAG_LEN_MIN,
      maxLength = TAG_LEN_MAX * 2
    )
    _tagNamePrepareM(m0)
  }

  /** Маппинг экземпляра тег исходя из имени тега. */
  def tagNameAsTagM: Mapping[MNodeTag] = {
    tagNameM.transform [MNodeTag] (_tagName2tag, _.raw)
  }

  /** Маппинг для строки, в которой может быть задано сразу несколько тегов. */
  def newTagsM: Mapping[Seq[MNodeTag]] = {
    nonEmptyText(minLength = TAG_LEN_MIN, maxLength = 256)
      .transform[ Seq[MNodeTag] ](
        {allRaw =>
          val lenMin = TAG_LEN_MIN
          val lenMax = TAG_LEN_MAX
          allRaw.split("[,;|]")
            .iterator
            .map { _tagNamePrepare }
            .filter(_.length >= lenMin)
            .filter(_.length <= lenMax)
            .map(_tagName2tag)
            .toSeq
        },
        _.mkString(", ")
      )
      .verifying("error.required", _.nonEmpty)
  }

  /** Маппинг для множественных значений поля тегов. */
  def tagsMapM: Mapping[TagsMap_t] = {
    list(tagNameAsTagM)
      .transform [TagsMap_t] (
        {tags =>
          tags.iterator
            .map { t => t.id -> t }
            .toMap
        },
        {tmap =>
          tmap.valuesIterator
            .toList
            .sortBy(_.id)
        }
      )
  }

  def tagsMapKM = TAGS_K -> tagsMapM


  /** apply-функция для формы добавления/редактировать рекламной карточки.
    * Вынесена за пределы генератора ad-маппингов во избежание многократного создания в памяти экземпляров функции. */
  def adFormApply(userCatId: Set[String], bmr: BlockMapperResult, pattern: Option[String],
                  richDescrOpt: Option[RichDescr], bgColor: String, tags: TagsMap_t): AdFormMResult = {
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
      richDescrOpt = richDescrOpt,
      tags        = tags
    )
    mad -> bmr.bim
  }

  /** Функция разборки для маппинга формы добавления/редактирования рекламной карточки. */
  def adFormUnapply(applied: AdFormMResult): Option[(Set[String], BlockMapperResult, Option[String], Option[RichDescr], String, TagsMap_t)] = {
    val mad = applied._1
    val bmr = BlockMapperResult(mad, applied._2)
    val pattern = mad.colors.get(AdColorFns.WIDE_IMG_PATTERN_COLOR_FN.name)
    import AdColorFns._
    val bgColor = mad.colors.getOrElse(IMG_BG_COLOR_FN.name, IMG_BG_COLOR_FN.default)
    Some( (mad.userCatId, bmr, pattern, mad.richDescrOpt, bgColor, mad.tags) )
  }


  /**
   * Сборщик форм произвольного назначения для парсинга реквестов с данными рекламной карточки.
   * @return Маппинг формы, готовый к эксплуатации.
   */
  def getAdFormM(): AdFormM = {
    Form(
      mapping(
        CAT_ID_K    -> adCatIdsM,
        OFFER_K     -> BlocksConf.DEFAULT.strictMapping,
        PATTERN_K   -> coveringPatternM,
        DESCR_K     -> richDescrOptM,
        BG_COLOR_K  -> colorM,
        TAGS_K      -> tagsMapM
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

