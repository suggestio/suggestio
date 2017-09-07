package io.suggest.quill.v

import com.quilljs.quill.modules.formats.{Font, Size}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.css.ScalaCssDefaults._

import scalacss.internal.DslBase.ToStyle
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.17 16:11
  * Description: Особые статические стили для quill-редактора.
  *
  * Редактор автоматом генерит названия стилей на основе передаваемых ему данных,
  * а мы тут под эти названия подстраиваемся.
  */
class QuillCss extends StyleSheet.Standalone {

  import dsl._

  final private val NAME_DELIM = "-"

  private val QL_ = ".ql" + NAME_DELIM

  private val QL_SNOW_CSS_SEL = QL_ + "snow"

  private val QL_PICKER_CSS_SEL = QL_ + "picker"

  private val LABEL = "label"
  private val ITEM = "item"

  private val VALUE_ATTR_NAME = HtmlConstants.ATTR_PREFIX + "value"


  // Отрендерить общие стили отображения всех шрифтов.
  for {
    (suffix, fsz)  <- List(
      ITEM  -> 20,
      LABEL -> 18
    )
  } {
    QL_SNOW_CSS_SEL - (
      &(QL_PICKER_CSS_SEL + QL_ + Font.FONT) - (
        &(QL_PICKER_CSS_SEL + NAME_DELIM + suffix).before - (
          fontSize( fsz.px )
        )
      )
    )
  }


  // Отрендерить стили для MFonts.
  for (mfont <- MFonts.valuesT) {

    val xFontFamilyName = mfont.cssFontFamily

    val fontFamilyAV = {
      fontFamily.attr := Css.quoted( xFontFamilyName )
    }

    /* MFonts:
     *  // For quill wysiwyg editor: fonts support
     * .ql-font-{name}
     *   font-family: name
     */
    s"$QL_${Font.FONT}$NAME_DELIM$xFontFamilyName" - (
      fontFamilyAV
    )

    val pickerCssAttrs = List[ToStyle](
      fontFamilyAV,
      content := Css.quoted( xFontFamilyName )
    )

    /* MFonts:
     * .ql-snow .ql-picker.ql-font
     *   .ql-picker-item[data-value={name}]::before, .ql-picker-label[data-value={name}]::before
     *     font-family: name
     *     content: name
     */
    QL_SNOW_CSS_SEL - (
      &(QL_PICKER_CSS_SEL + QL_ + Font.FONT) - (

        // TODO Как объединить/дедублицировать селекторы воедино? Они ведь почти одинаковые.
        &(QL_PICKER_CSS_SEL + NAME_DELIM + ITEM).attr(VALUE_ATTR_NAME, xFontFamilyName).before - (
          pickerCssAttrs: _*
        ),
        &(QL_PICKER_CSS_SEL + NAME_DELIM + LABEL).attr(VALUE_ATTR_NAME, xFontFamilyName).before - (
          pickerCssAttrs: _*
        )

      )
    )

  }


  // Отрендерить стили для размеров шрифтов.
  for (mFontSize <- MFontSizes.values) {

    val sizeStr = mFontSize.value.toString
    val lineHeightPx = mFontSize.lineHeight.px

    val labelCssAttrs = List[ToStyle](
      content := Css.quoted( sizeStr )
    )

    val fontSizeAV: ToStyle = fontSize( mFontSize.value.px )
    val lineHeightAV: ToStyle = lineHeight( lineHeightPx )

    val itemCssAttrs: List[ToStyle] =
      fontSizeAV ::
        lineHeightAV ::
        labelCssAttrs

    s"$QL_${Size.SIZE}$NAME_DELIM$sizeStr" - (
      fontSizeAV,
      lineHeightAV
    )

    QL_SNOW_CSS_SEL - (
      &(QL_PICKER_CSS_SEL + QL_ + Size.SIZE) - (

        // TODO Как объединить/дедублицировать селекторы воедино? Они ведь почти одинаковые.
        &(QL_PICKER_CSS_SEL + NAME_DELIM + ITEM).attr(VALUE_ATTR_NAME, sizeStr).before - (
          itemCssAttrs: _*
        ),
        &(QL_PICKER_CSS_SEL + NAME_DELIM + LABEL).attr(VALUE_ATTR_NAME, sizeStr).before - (
          labelCssAttrs: _*
        )

      )
    )

  }





}
