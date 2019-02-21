package io.suggest.font

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.11.14 12:16
  * Description: Модель шрифтов. До её появления шрифты были захардкожены прямо в шаблонах.
  * initial = 0 -- это проверка испрользуется в adFormBase на "первость" элемента модели.
  *
  * 2017.aug.22: Решено явно разделить названия css-классов, файлов и id элементов модели,
  * заодно переведя модель на enumeratum.
  * Но тут есть проблема: надо сначала портировать все карточки на dtags,
  * распарсив rich descr html тоже в dtags, написав весь рендер (в т.ч. tinyMCE с маппингом css <=> id),
  * и только потом можно сделать явное окончательное разделение и портирование,
  * выкинув устаревшие части этой модели.
  */

object MFonts extends StringEnum[MFont] {

  // Экземпляры шрифтов в АЛФАВИТНОМ ПОРЯДКЕ

  case object AaHigherup extends MFont("aahu") {
    override def fileName = "aa-higherup"
    override def descr  = "Higherup"
    //override def _names = "aa_higherup-webfont" :: super._names
  }

  case object BlocExtCond extends MFont("blecc") {
    override def fileName = "bloc-ext-con-c"
    override def descr  = "BlocExt Cond"
    //override def _names = "blocextconc-webfont" :: super._names
  }

  case object BodonConc extends MFont("bocc") {
    override def fileName = "bodon-con-c"
    override def descr  = "Bodon Conc"
    //override def _names = "bodonconc-webfont" :: super._names
  }

  case object Confic extends MFont("cf") {
    override def fileName = "confic"
    override def descr  = "Confic"
    //override def _names = "confic-webfont" :: super._names
  }

  case object FavCondCBold extends MFont("fvccb") {
    override def fileName = "favorit-cond-c-bold"
    override def descr  = "Favorit Cond C Bold"
    //override def _names = "favoritcondc-bold-webfont" :: super._names
  }

  case object FavLightCondCReg extends MFont("fvlccr") {
    override def fileName = "favorit-light-cond-c-regular"
    override def descr  = "Favorit Light Cond C Regular"
    //override def _names = "favoritlightcondcregular" :: super._names
  }

  case object FuturaFuturisC extends MFont("fufuc") {
    override def fileName = "futura-futuris-c"
    override def descr  = "Futur Fut C"
    //override def _names = "futurfutc-webfont" :: super._names
  }

  case object GazTransport extends MFont("gzt") {
    override def fileName = "gaz-transport"
    override def descr  = "GAZ Transport"
  }

  case object Georgia extends MFont("g") {
    override def fileName = "Georgia"
    override def descr  = fileName
  }

  case object HeliosCondLight extends MFont("hecl") {
    override def fileName = "helios-cond-light"
    override def descr  = "Helios Cond Light"
    //override def _names = "helioscondlight-webfont" :: super._names
  }

  case object HeliosThin extends MFont("het") {
    override def fileName = "helios-thin"
    override def descr  = "Helios Thin"
    //override def _names = "heliosthin" :: super._names
  }

  case object NewspaperSans extends MFont("npsr") {
    override def fileName = "news-paper-sans-regular"
    override def descr  = "Newspaper Sans"
    //override def _names = "newspsan-webfont" :: super._names
  }

  case object Meloranic extends MFont("mr") {
    override def fileName = "meloriac"
    override def descr  = "Meloriac"
  }

  case object OpenSansLight extends MFont("osl") {
    override def fileName = "opensans-light"
    override def descr  = "OpenSans Light Regular"
  }

  case object OpenSansRegular extends MFont("osr") {
    override def fileName = "opensans-regular"
    override def descr  = "OpenSans Regular"
  }

  case object OctinTeamHeavy extends MFont("oth") {
    override def fileName = "octin-team-heavy"
    override def descr  = "Octin Team Heavy"
  }

  case object Perforama extends MFont("pr") {
    override def fileName = "perforama"
    override def descr  = "Perforama"
    //override def _names = "perforama-webfont" :: super._names
  }

  case object PfDinTextCompProMed extends MFont("pdcpm") {
    override def fileName = "pf-din-comp-pro-medium"
    override def descr  = "PF Din Text Comp Pro Medium"
    //override def _names = "PFDinTextCompPro-Medium" :: super._names
  }

  case object PharmadinCondLight extends MFont("pcl") {
    override def fileName = "pharmadin-condensed-light"
    override def descr  = "Pharmadin Condensed Light"
    //override def _names = "PharmadinCondensedLight" :: super._names
  }

  case object Posterboard extends MFont("pb") {
    override def fileName = "posterboard"
    override def descr  = "Posterboard"
  }

  case object PtSansNarrow extends MFont("psn") {
    override def fileName = "pt-sans-narrow"
    override def descr  = "PT Sans Narrow"
    //override def _names = "PT_Sans-Narrow" :: super._names
  }

  case object RexBold extends MFont("rxb") {
    override def fileName = "rex-bold"
    override def descr  = "Rex Bold"
    //override def _names = "rex_bold-webfont" :: super._names
  }


  // Конец перечисления шрифтов в АЛФАВИТНОМ ПОРЯДКЕ.


  // values почти не используется напрямую (за искл. QuillInit и одноразового QuillCss).
  override def values = findValues

  /** Дефолтовый шрифт. */
  def default: MFont = HeliosThin


  /** На текущий момент, quill используется именование на основе css-классов,
    * поэтому тут lazy val, чтобы quill мог ориентироваться, а остальных это не касалось.
    * Это можно обойти, убрав quill, либо отрефакторив названия классов в styl-css-файлах.
    */
  lazy val cssClass2FontMap: Map[String, MFont] = {
    val iter = for {
      v <- values.iterator
    } yield {
      v.cssFontFamily -> v
    }
    iter.toMap
  }

  def withCssClass(fileName: String): MFont = cssClass2FontMap(fileName)
  def withCssClassOpt(fileName: String): Option[MFont] = cssClass2FontMap.get(fileName)

}


sealed abstract class MFont(override val value: String) extends StringEnumEntry {

  def fileName: String

  /** Название CSS font-family. */
  final def cssFontFamily: String = fileName

  /** Отображаемое название шрифта. */
  def descr: String

}

object MFont {

  implicit def mFontFormat: Format[MFont] =
    EnumeratumUtil.valueEnumEntryFormat( MFonts )

  @inline implicit def univEq: UnivEq[MFont] = UnivEq.derive

}
