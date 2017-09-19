package util.ad

import javax.inject.Singleton

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.blk.ent.{EntFont, TextEnt}
import io.suggest.ad.edit.m.MAdEditForm
import io.suggest.ad.form.AdFormConstants._
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.font.{MFont, MFontSize, MFontSizes, MFonts}
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdEditEdge
import io.suggest.jd.tags._
import io.suggest.model.n2.ad.rd.RichDescr
import io.suggest.model.n2.ad.MNodeAd
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.{MNode, MNodeTypes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.colors.{MColorData, MColors}
import io.suggest.model.n2.node.meta.{MBasicMeta, MBusinessInfo, MMeta}
import io.suggest.text.{MTextAlign, MTextAligns}
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.ym.model.ad.AdColorFns
import models.blk.ed.{AdFormM, AdFormResult, BindResult}
import models.mctx.Context
import play.api.data.Forms._
import play.api.data._
import util.FormUtil._
import util.TplDataFormatUtil
import util.blocks.BlocksConf

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:15
 * Description: Общая утиль для работы с разными ad-формами: preview и обычными.
 */
@Singleton
class LkAdEdFormUtil extends MacroLogsImpl {

  /** Маппинг для выравнивания текста в рамках поля. */
  def textAlignOptM: Mapping[Option[MTextAlign]] = {
    optional(text(maxLength = 10))
      .transform[Option[MTextAlign]](
        {_.filter(_.length <= 10)
          .flatMap { s =>
            if (s.length == 1) {
              MTextAligns.withValueOpt( s )
            } else if (s.length == 0) {
              None
            } else {
              MTextAligns.withCssNameOption(s)
            }
          }
        },
        { _.map(_.cssName) }
      )
  }


  /** Маппинг для размера шрифта. */
  def fontSizeM: Mapping[MFontSize] = {
    number(min = MFontSizes.min.value, max = MFontSizes.max.value)
      .transform [Option[MFontSize]] (MFontSizes.withValueOpt, _.getOrElse(MFontSizes.min).value)
      .verifying("error.unavailable.font.size", _.isDefined)
      .transform[MFontSize](_.get, Some.apply)
  }


  private def _fontFamilyOptM: Mapping[Option[MFont]] = {
    text(maxLength = 32)
      .transform [Option[MFont]] (
        MFonts.maybeWithName,
        _.fold("")(_.strId)
      )
  }

  /** Маппер для значения font.family. */
  def fontFamilyOptM: Mapping[Option[MFont]] = {
    optional( _fontFamilyOptM )
      .transform[Option[MFont]](_.flatten, Some.apply )
  }

  def fontFamily: Mapping[MFont] = {
    _fontFamilyOptM
      .verifying("error.font.unknown", _.nonEmpty)
      .transform(_.get, Some.apply)
  }


  /** Маппер для описания, прилагаемого к рекламной карточке. */
  val richDescrOptM: Mapping[Option[RichDescr]] = {
    val rdTextM = text(maxLength = 500000)
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
        size   = Some(fsz),
        align  = align,
        family = family
      )
    }
    {aoff =>
      val fsz: MFontSize = aoff.size
        .getOrElse { MFontSizes.min }
      import aoff._
      Some((color, fsz, align, aoff.family))
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
  def coords2DM: Mapping[MCoords2di] = {
    // сохраняем маппинг в переменную на случай если coordM станет def вместо val.
    val _coordM = coordM
    mapping(
      "x" -> _coordM,
      "y" -> _coordM
    )
    { MCoords2di.apply }
    { MCoords2di.unapply }
  }
  def coords2DOptM: Mapping[Option[MCoords2di]] = optional(coords2DM)


  /** Маппим строковое поле с настройками шрифта. */
  def aoStringFieldM(m: Mapping[String], fontM: Mapping[EntFont]): Mapping[TextEnt] = {
    // TODO "coords"  -> coords2DOptM
    mapping(
      "value" -> m,
      "font"  -> fontM
    )
    { TextEnt.apply }
    { TextEnt.unapply }
  }


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
        ),
        business = MBusinessInfo(
          siteUrl = bmr.href
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
      bim       = applied.bim,
      href      = mad.meta.business.siteUrl
    )
    val pattern = mad.meta.colors.pattern
    val bgColor = mad.meta.colors.bg
      .getOrElse( MColorData( AdColorFns.IMG_BG_COLOR_FN.default) )
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


  //---------------------------------------------------------------------------
  // v2 react form

  /** Срендерить и вернуть дефолтовый документ пустой карточки для текущего языка. */
  def defaultEmptyDocument(implicit ctx: Context): Future[MAdEditForm] = {
    // TODO Брать готовую карточку из какого-то узла и пробегаться по эджам с использованием messages.

    // Тут просто очень временный документ.
    // Потом надо будет просто искать карточки на определённом узле, и возвращать их документ
    // и эджи (эджи - прорендерив через ctx.messages). Карточки сгенерить через новый редактор (который ещё не написан).
    val upperBlockEdgeId = 1
    val alsoDisplayedInGridEdgeId = upperBlockEdgeId + 1

    val descriptionEdgeId = alsoDisplayedInGridEdgeId + 1
    val descrContentEdgeId = descriptionEdgeId + 1

    val fr3text1EdgeId = descrContentEdgeId + 1
    val fr3text2EdgeId = fr3text1EdgeId + 1

    val w1 = BlockWidths.default
    val h1 = BlockHeights.default

    val textPred = MPredicates.JdContent.Text

    val r = MAdEditForm(
      template = IDocTag.document(
        // Strip1 содержит намёк на то, что это верхний блок.
        IDocTag.strip(
          bm = BlockMeta(
            w = w1,
            h = h1,
            wide = true
          ),
          bgColor = Some(MColorData(
            code = "060d45"
          ))
        )(
          // Надпись "Верхний блок"
          IDocTag.edgeQd(upperBlockEdgeId, MCoords2di(x = w1.value, y = h1.value) / 3),

          // Надпись "также отображается в плитке"
          IDocTag.edgeQd( alsoDisplayedInGridEdgeId, MCoords2di(x = w1.value/3*2, y = h1.value / 2) )
        ),

        // strip2 содержит предложение добавить описание или что-то ещё.
        IDocTag.strip(
          bm = BlockMeta(
            w = w1,
            h = BlockHeights.H140,
            wide = true
          ),
          bgColor = Some(MColorData(
            code = "bcf014"
          ))
        )(
          IDocTag.edgeQd( descriptionEdgeId,  MCoords2di(5,  10) ),
          IDocTag.edgeQd( descrContentEdgeId, MCoords2di(33, 50) )
        ),

        IDocTag.strip(
          bm = BlockMeta(
            w = w1,
            h = BlockHeights.H460,
            wide = true
          ),
          bgColor = Some(MColorData(
            code = "111111"
          ))
        )(
          IDocTag.edgeQd( fr3text1EdgeId, MCoords2di(15, 200) ),
          IDocTag.edgeQd( fr3text2EdgeId, MCoords2di(35, 400) )
        )
      ),
      edges = Seq(
        // strip1
        MJdEditEdge(
          predicate = textPred,
          id        = upperBlockEdgeId,
          text      = Some( ctx.messages( MsgCodes.`Upper.block` ) + "\n" ),
        ),
        MJdEditEdge(
          predicate = textPred,
          id        = alsoDisplayedInGridEdgeId,
          text      = Some( ctx.messages( MsgCodes.`also.displayed.in.grid` ) + "\n" )
        ),

        // strip2
        MJdEditEdge(
          predicate = textPred,
          id        = descriptionEdgeId,
          text      = Some( ctx.messages( MsgCodes.`Description` ) + "\n" )
        ),
        MJdEditEdge(
          predicate = textPred,
          id        = descrContentEdgeId,
          text      = Some( "aw efawfwae fewafewa feawf aew rtg rs5y 4ytsg ga\n" )
        ),

        // strip3
        MJdEditEdge(
          predicate = textPred,
          id        = fr3text1EdgeId,
          text      = Some( "lorem ipsum und uber blochHeight wr2 34t\n" )
        ),
        MJdEditEdge(
          predicate = textPred,
          id        = fr3text2EdgeId,
          text      = Some( "webkit-transition: transform 0.2s linear\n" )
        )
      )
    )

    Future.successful(r)
  }

}


/** Интерфейс для DI-поля с инстаном [[LkAdEdFormUtil]]. */
trait ILkAdEdFormUtil {
  def marketAdFormUtil: LkAdEdFormUtil
}

