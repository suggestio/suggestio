package models.blk

import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.{JsString, JsObject, JsArray}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.14 12:16
 * Description: Модель шрифтов. До её появления шрифты были захардкожены прямо в шаблонах.
 * initial = 0 -- это проверка испрользуется в adFormBase на "первость" элемента модели.
 */
object Fonts extends Enumeration(0) {

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
  type Font = Val

  val FavLightCondCReg    : Font = Val("favoritlightcondcregular", "Favorit Light Cond C Regular")
  val FavCondCBold        : Font = Val("favoritcondc-bold-webfont", "Favorit Cond C Bold")
  val HeliosThin          : Font = Val("heliosthin", "Helios Thin")
  val HeliosCondLight     : Font = Val("helioscondlight-webfont", "Helios Cond Light")
  val HeliosExtBlack      : Font = Val("HeliosExtBlack", "Helios Ext Black")
  val PfDinTextCompProMed : Font = Val("PFDinTextCompPro-Medium", "PF Din Text Comp Pro Medium")
  val FuturFutC           : Font = Val("futurfutc-webfont", "Futur Fut C")
  val PharmadinCondLight  : Font = Val("PharmadinCondensedLight", "Pharmadin Condensed Light")
  val NewspaperSans       : Font = Val("newspsan-webfont", "Newspaper Sans")
  val RexBold             : Font = Val("rex_bold-webfont", "Rex Bold")
  val Preforama           : Font = Val("perforama-webfont", "Perforama")
  val DecorC              : Font = Val("decorc-webfont", "Decor C")
  val BlocExtCond         : Font = Val("blocextconc-webfont", "BlocExt Cond")
  val BodonConc           : Font = Val("bodonconc-webfont", "Bodon Conc")
  val Higherup            : Font = Val("aa_higherup-webfont", "Higherup")
  val Georgia             : Font = Val("Georgia", "Georgia")
  val Confic              : Font = Val("confic-webfont", "Confic")


  /** Приведение Enumeration.Value к экземпляру модели Font. */
  implicit def value2val(x: Value): Font = x.asInstanceOf[Font]

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
