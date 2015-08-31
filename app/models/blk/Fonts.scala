package models.blk

import io.suggest.common.menum.{EnumMaybeWithName, EnumValue2Val}
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.{JsString, JsObject, JsArray}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.14 12:16
 * Description: Модель шрифтов. До её появления шрифты были захардкожены прямо в шаблонах.
 * initial = 0 -- это проверка испрользуется в adFormBase на "первость" элемента модели.
 */
object Fonts extends Enumeration(0) with EnumValue2Val with EnumMaybeWithName {

  /**
   * Класс-экземпляр модели.
   * @param fileName base filename шрифта.
   * @param descr Читабельное описание.
   */
  sealed protected case class Val(fileName: String, descr: String) extends super.Val(fileName) {
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
  }


  /** Тип экземпляра модели. */
  override type T = Val

  val FavLightCondCReg    : T = Val("favoritlightcondcregular",     "Favorit Light Cond C Regular")
  val FavCondCBold        : T = Val("favoritcondc-bold-webfont",    "Favorit Cond C Bold")
  val HeliosThin          : T = Val("heliosthin",                   "Helios Thin")
  val HeliosCondLight     : T = Val("helioscondlight-webfont",      "Helios Cond Light")
  val PfDinTextCompProMed : T = Val("PFDinTextCompPro-Medium",      "PF Din Text Comp Pro Medium")
  val FuturFutC           : T = Val("futurfutc-webfont",            "Futur Fut C")
  val PharmadinCondLight  : T = Val("PharmadinCondensedLight",      "Pharmadin Condensed Light")
  val NewspaperSans       : T = Val("newspsan-webfont",             "Newspaper Sans")
  val RexBold             : T = Val("rex_bold-webfont",             "Rex Bold")
  val Preforama           : T = Val("perforama-webfont",            "Perforama")
  val BlocExtCond         : T = Val("blocextconc-webfont",          "BlocExt Cond")
  val BodonConc           : T = Val("bodonconc-webfont",            "Bodon Conc")
  val Higherup            : T = Val("aa_higherup-webfont",          "Higherup")
  val Georgia             : T = Val("Georgia",                      "Georgia")
  val Confic              : T = Val("confic-webfont",               "Confic")

  val GazTransport        : T = Val("gaz-transport",                "GAZ Transport")
  val Meloranic           : T = Val("meloriac",                     "Meloriac")
  val OctinTeamHeavy      : T = Val("octin-team-heavy",             "Octin Team Heavy")
  val Posterboard         : T = Val("posterboard",                  "Posterboard")
  val PtSansNarrow        : T = Val("pt-sans-narrow",               "PT Sans Narrow")
  val OpenSansLight       : T = Val("opensans-light",               "OpenSans Light Regular")

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

}
