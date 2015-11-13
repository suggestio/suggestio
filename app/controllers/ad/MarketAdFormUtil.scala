package controllers.ad

import io.suggest.model.n2.ad.{MNodeAd, ent}
import io.suggest.model.n2.ad.rd.RichDescr
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.MBasicMeta
import io.suggest.model.n2.node.meta.colors.{MColorData, MColors}
import models.MColorData
import models.blk.AdColorFns
import models._
import models.blk._
import models.blk.ed.{AdFormM, AdFormResult, BindResult}
import util.FormUtil._
import play.api.data._, Forms._
import io.suggest.ad.form.AdFormConstants._
import util.{PlayMacroLogsImpl, TplDataFormatUtil}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:15
 * Description: Общая утиль для работы с разными ad-формами: preview и обычными.
 */
object MarketAdFormUtil extends PlayMacroLogsImpl {

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
  val richDescrOptM: Mapping[Option[RichDescr]] = {
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
  def fontM: Mapping[EntFont] = {
    mapping(
      "color"  -> colorM,
      "size"   -> fontSizeM,
      "align"  -> textAlignOptM,
      "family" -> fontFamilyOptM
    )
    {(color, fsz, align, family) =>
      EntFont(
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
  val coveringPatternM: Mapping[Option[MColorData]] = {
    tuple(
      "enabled" -> boolean,
      "color"   -> colorDataOptM
    )
    .verifying("error.required", {m => m match {
      case (true, None)   => false
      case _              => true
    }})
    .transform[Option[MColorData]] (
      { case (isEnabled, colorOpt) =>
        colorOpt.filter(_ => isEnabled)
      },
      {colorOpt =>
        (colorOpt.isDefined, colorOpt)
      }
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
  def coords2DM: Mapping[ent.Coords2d] = {
    // сохраняем маппинг в переменную на случай если coordM станет def вместо val.
    val _coordM = coordM
    mapping(
      "x" -> _coordM,
      "y" -> _coordM
    )
    { Coords2d.apply }
    { Coords2d.unapply }
  }
  def coords2DOptM: Mapping[Option[ent.Coords2d]] = optional(coords2DM)


  /** Маппим строковое поле с настройками шрифта. */
  def aoStringFieldM(m: Mapping[String], fontM: Mapping[EntFont], withCoords: Boolean): Mapping[TextEnt] = {
    if (withCoords) {
      mapping(
        "value"   -> m,
        "font"    -> fontM,
        "coords"  -> coords2DOptM
      )
      { TextEnt.apply }
      { TextEnt.unapply }
    } else {
      mapping(
        "value" -> m,
        "font"  -> fontM
      )
      { TextEnt.apply(_, _) }
      { aosf => TextEnt.unapply(aosf).map { u => u._1 -> u._2 } }
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

  private def _tagName2tag(tname: String): MTagEdge = {
    MTagEdge(
      face = tname
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
  def tagNameAsTagM: Mapping[MTagEdge] = {
    tagNameM.transform [MTagEdge] (_tagName2tag, _.face)
  }

  /** Маппинг для строки, в которой может быть задано сразу несколько тегов. */
  def newTagsM: Mapping[Seq[MTagEdge]] = {
    nonEmptyText(minLength = TAG_LEN_MIN, maxLength = 256)
      .transform[ Seq[MTagEdge] ](
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
  val tagsMapM: Mapping[TagsMap_t] = {
    list(tagNameAsTagM)
      .transform [TagsMap_t] (
        MTagEdge.tags2map,
        MTagEdge.map2sortedTags
      )
  }

  def tagsMapKM = TAGS_K -> tagsMapM


  /** apply-функция для формы добавления/редактировать рекламной карточки.
    * Вынесена за пределы генератора ad-маппингов во избежание многократного создания в памяти экземпляров функции. */
  def adFormApply(bmr: BindResult, pattern: Option[MColorData],
                  richDescrOpt: Option[RichDescr], bgColor: MColorData): AdFormResult = {
    val mad = MNode(
      common = MNodeCommon(
        ntype = MNodeTypes.Ad,
        isDependent = true
      ),
      meta = MMeta(
        basic = MBasicMeta(
          // Сгенерить неиндексируемое имя карточки на основе полей.
          techName = Some {
            val s = bmr.entites
              .iterator
              .flatMap(_.text)
              .map(_.value)
              .filter(_.length >= 2)
              .toSet
              .mkString(" | ")
            TplDataFormatUtil.strLimitLen(s, 32)
          }
        ),
        colors = MColors(
          pattern = pattern,
          bg      = Some( bgColor )
        )
      ),
      ad = MNodeAd(
        entities  = MNodeAd.toEntMap1( bmr.entites ),
        richDescr = richDescrOpt,
        blockMeta = Some(bmr.blockMeta)
      )
    )
    AdFormResult(mad, bmr.bim)
  }

  /** Функция разборки для маппинга формы добавления/редактирования рекламной карточки. */
  def adFormUnapply(applied: AdFormResult): Option[(BindResult, Option[MColorData], Option[RichDescr], MColorData)] = {
    val mad = applied.mad
    val bmr = BindResult(
      entites   = mad.ad.entities.valuesIterator.toList,
      blockMeta = mad.ad.blockMeta.getOrElse {
        LOGGER.warn("adFormUnapply(): BlockMeta is missing on ad[" + mad.id + "]")
        BlockMeta.DEFAULT
      },
      bim       = applied.bim
    )
    val pattern = mad.meta.colors.pattern
    import AdColorFns._
    val bgColor = mad.meta.colors.bg
      .getOrElse( MColorData(IMG_BG_COLOR_FN.default) )
    Some( (bmr, pattern, mad.ad.richDescr, bgColor) )
  }


  /**
   * Сборщик форм произвольного назначения для парсинга реквестов с данными рекламной карточки.
   * @return Маппинг формы, готовый к эксплуатации.
   */
  // TODO Сделать val наверное надо... Но есть циклическая зависимость между этим классом и BfText.
  def adFormM: AdFormM = {
    Form(
      mapping(
        OFFER_K     -> BlocksConf.DEFAULT.strictMapping,
        PATTERN_K   -> coveringPatternM,
        DESCR_K     -> richDescrOptM,
        BG_COLOR_K  -> colorDataM
      )(adFormApply)(adFormUnapply)
    )
  }


}

