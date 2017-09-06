package io.suggest.font

import io.suggest.common.menum.{EnumJsonReadsT, EnumMaybeWithMultiNameMap, EnumValue2Val}
import io.suggest.primo.IStrId
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

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

object MFonts
  extends Enumeration(0)
  with EnumMaybeWithMultiNameMap
  with EnumValue2Val
  with EnumJsonReadsT
{

  /** Трейт экземпляра модели. */
  sealed protected[this] trait ValT extends super.ValT with IStrId {

    /**
     * tinymce принимает данные по шрифту в style_formats в таком формате.
     * @param tail Дополнительные json-поля.
     * @return JsObject для передачи в init() style_formats.
     */
    def toJsonTinyMce(tail: List[(String, JsValue)] = Nil): JsObject = {
      val title = "title" -> JsString(descr)
      val styles = "styles" -> JsObject(Seq(
        "font-family" -> JsString(cssFontFamily)
      ))
      JsObject(title :: styles :: tail)
    }

    def fileName: String

    /** Название CSS font-family. */
    def cssFontFamily: String = fileName

    /** Отображаемое название шрифта. */
    def descr: String

    /** Имена прошлые и текущие. */
    override def _names = fileName :: strId :: Nil

  }


  /**
   * Класс-экземпляр модели.
   */
  abstract sealed protected[this] class Val(override val strId: String)
    extends super.Val(strId)
    with ValT


  /** Тип экземпляра модели. */
  override type T = Val


  // Экземпляры шрифтов в АЛФАВИТНОМ ПОРЯДКЕ

  val AaHigherup          : T = new Val("aahu") {
    override def fileName = "aa-higherup"
    override def descr  = "Higherup"
    override def _names = "aa_higherup-webfont" :: super._names
  }

  val BlocExtCond         : T = new Val("blecc") {
    override def fileName = "bloc-ext-con-c"
    override def descr  = "BlocExt Cond"
    override def _names = "blocextconc-webfont" :: super._names
  }

  val BodonConc           : T = new Val("bocc") {
    override def fileName = "bodon-con-c"
    override def descr  = "Bodon Conc"
    override def _names = "bodonconc-webfont" :: super._names
  }

  val Confic              : T = new Val("cf") {
    override def fileName = "confic"
    override def descr  = "Confic"
    override def _names = "confic-webfont" :: super._names
  }

  val FavCondCBold        : T = new Val("fvccb") {
    override def fileName = "favorit-cond-c-bold"
    override def descr  = "Favorit Cond C Bold"
    override def _names = "favoritcondc-bold-webfont" :: super._names
  }

  val FavLightCondCReg    : T = new Val("fvlccr") {
    override def fileName = "favorit-light-cond-c-regular"
    override def descr  = "Favorit Light Cond C Regular"
    override def _names = "favoritlightcondcregular" :: super._names
  }

  val FuturaFuturisC      : T = new Val("fufuc") {
    override def fileName = "futura-futuris-c"
    override def descr  = "Futur Fut C"
    override def _names = "futurfutc-webfont" :: super._names
  }

  val GazTransport        : T = new Val("gzt") {
    override def fileName = "gaz-transport"
    override def descr  = "GAZ Transport"
  }

  val Georgia             : T = new Val("g") {
    override def fileName = "Georgia"
    override def descr  = fileName
  }

  val HeliosCondLight     : T = new Val("hecl") {
    override def fileName = "helios-cond-light"
    override def descr  = "Helios Cond Light"
    override def _names = "helioscondlight-webfont" :: super._names
  }

  val HeliosThin          : T = new Val("het") {
    override def fileName = "helios-thin"
    override def descr  = "Helios Thin"
    override def _names = "heliosthin" :: super._names
  }

  val NewspaperSans       : T = new Val("npsr") {
    override def fileName = "news-paper-sans-regular"
    override def descr  = "Newspaper Sans"
    override def _names = "newspsan-webfont" :: super._names
  }

  val Meloranic           : T = new Val("mr") {
    override def fileName = "meloriac"
    override def descr  = "Meloriac"
  }

  val OpenSansLight       : T = new Val("osl") {
    override def fileName = "opensans-light"
    override def descr  = "OpenSans Light Regular"
  }

  val OpenSansRegular      : T = new Val("osr") {
    override def fileName = "opensans-regular"
    override def descr  = "OpenSans Regular"
  }

  val OctinTeamHeavy      : T = new Val("oth") {
    override def fileName = "octin-team-heavy"
    override def descr  = "Octin Team Heavy"
  }

  val Perforama           : T = new Val("pr") {
    override def fileName = "perforama"
    override def descr  = "Perforama"
    override def _names = "perforama-webfont" :: super._names
  }

  val PfDinTextCompProMed : T = new Val("pdcpm") {
    override def fileName = "pf-din-comp-pro-medium"
    override def descr  = "PF Din Text Comp Pro Medium"
    override def _names = "PFDinTextCompPro-Medium" :: super._names
  }

  val PharmadinCondLight  : T = new Val("pcl") {
    override def fileName = "pharmadin-condensed-light"
    override def descr  = "Pharmadin Condensed Light"
    override def _names = "PharmadinCondensedLight" :: super._names
  }

  val Posterboard         : T = new Val("pb") {
    override def fileName = "posterboard"
    override def descr  = "Posterboard"
  }

  val PtSansNarrow        : T = new Val("psn") {
    override def fileName = "pt-sans-narrow"
    override def descr  = "PT Sans Narrow"
    override def _names = "PT_Sans-Narrow" :: super._names
  }

  val RexBold             : T = new Val("rxb") {
    override def fileName = "rex-bold"
    override def descr  = "Rex Bold"
    override def _names = "rex_bold-webfont" :: super._names
  }


  // Конец перечисления шрифтов в АЛФАВИТНОМ ПОРЯДКЕ.


  /** Для рендера json-конфига tinyMCE лучше использовать этот метод. */
  def valuesJsonTinyMce: JsArray = {
    val inlineSpan = List(
      "inline" -> JsString("span")
    )
    // iterator + toSeq используется из-за того, что values() - это Set[], а не Seq.
    val jsons = values.iterator
      .map { _.toJsonTinyMce(inlineSpan) }
      .toSeq
    JsArray(jsons)
  }


  override protected val _nameMapVal = _nameMap

}
