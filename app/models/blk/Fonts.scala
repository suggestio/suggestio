package models.blk

import io.suggest.common.menum.{EnumValue2Val, EnumMaybeWithMultiNameMap}
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.{JsString, JsObject, JsArray}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.14 12:16
 * Description: Модель шрифтов. До её появления шрифты были захардкожены прямо в шаблонах.
 * initial = 0 -- это проверка испрользуется в adFormBase на "первость" элемента модели.
 */
object Fonts extends Enumeration(0) with EnumMaybeWithMultiNameMap with EnumValue2Val {

  /** Трейт экземпляра модели. */
  sealed protected[this] trait ValT extends super.ValT {
    /**
     * tinymce принимает данные по шрифту в style_formats в таком формате.
     * @param tail Дополнительные json-поля.
     * @return JsObject для передачи в init() style_formats.
     */
    def toJsonTinyMce(tail: FieldsJsonAcc = Nil): JsObject = {
      val title = "title" -> JsString(descr)
      val styles = "styles" -> JsObject(Seq(
        "font-family" -> JsString(fileName)
      ))
      JsObject(title :: styles :: tail)
    }

    /** Название CSS font-family. */
    def fileName: String
    /** Отображаемое название шрифта. */
    def descr: String
    /** Имена прошлые и текущие. */
    override def _names = List(fileName)
  }

  /**
   * Класс-экземпляр модели.
   * @param fileName base filename шрифта.
   */
  abstract sealed protected[this] class Val(override val fileName: String)
    extends super.Val(fileName)
    with ValT


  /** Тип экземпляра модели. */
  override type T = Val


  // Экземпляры шрифтов
  val FavLightCondCReg    : T = new Val("favorit-light-cond-c-regular") {
    override def descr  = "Favorit Light Cond C Regular"
    override def _names = "favoritlightcondcregular" :: super._names
  }

  val FavCondCBold        : T = new Val("favorit-cond-c-bold") {
    override def descr  = "Favorit Cond C Bold"
    override def _names = "favoritcondc-bold-webfont" :: super._names
  }

  val HeliosThin          : T = new Val("helios-thin") {
    override def descr  = "Helios Thin"
    override def _names = "heliosthin" :: super._names
  }

  val HeliosCondLight     : T = new Val("helios-cond-light") {
    override def descr  = "Helios Cond Light"
    override def _names = "helioscondlight-webfont" :: super._names
  }

  val PfDinTextCompProMed : T = new Val("PFDinTextCompPro-Medium") {
    override def descr  = "PF Din Text Comp Pro Medium"
  }

  val FuturFutC           : T = new Val("futura-futuris-c") {
    override def descr  = "Futur Fut C"
    override def _names = "futurfutc-webfont" :: super._names
  }

  val PharmadinCondLight  : T = new Val("PharmadinCondensedLight") {
    override def descr  = "Pharmadin Condensed Light"
  }

  val NewspaperSans       : T = new Val("newspsan-webfont") {
    override def descr  = "Newspaper Sans"
  }

  val RexBold             : T = new Val("rex_bold-webfont") {
    override def descr  = "Rex Bold"
  }

  val Perforama           : T = new Val("perforama-webfont") {
    override def descr  = "Perforama"
  }

  val BlocExtCond         : T = new Val("bloc-ext-con-c") {
    override def descr  = "BlocExt Cond"
    override def _names = "blocextconc-webfont" :: super._names
  }

  val BodonConc           : T = new Val("bodon-con-c") {
    override def descr  = "Bodon Conc"
    override def _names = "bodonconc-webfont" :: super._names
  }

  val Higherup            : T = new Val("aa-higherup") {
    override def descr  = "Higherup"
    override def _names = "aa_higherup-webfont" :: super._names
  }

  val Georgia             : T = new Val("Georgia") {
    override def descr  = "Georgia"
  }

  val Confic              : T = new Val("confic") {
    override def descr  = "Confic"
    override def _names = "confic-webfont" :: super._names
  }

  val GazTransport        : T = new Val("gaz-transport") {
    override def descr  = "GAZ Transport"
  }

  val Meloranic           : T = new Val("meloriac") {
    override def descr  = "Meloriac"
  }

  val OctinTeamHeavy      : T = new Val("octin-team-heavy") {
    override def descr  = "Octin Team Heavy"
  }

  val Posterboard         : T = new Val("posterboard") {
    override def descr  = "Posterboard"
  }

  val PtSansNarrow        : T = new Val("pt-sans-narrow") {
    override def descr  = "PT Sans Narrow"
    override def _names = "PT_Sans-Narrow" :: super._names
  }

  val OpenSansLight       : T = new Val("opensans-light") {
    override def descr  = "OpenSans Light Regular"
  }

  // TODO Добавить остальные шрифты из public/SIO/Fonts/. Портировать старые шрифты из fonts.styl.


  /** Для рендера json-конфига tinyMCE лучше использовать этот метод. */
  def valuesJsonTinyMce: JsArray = {
    val inlineSpan: FieldsJsonAcc = List(
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
