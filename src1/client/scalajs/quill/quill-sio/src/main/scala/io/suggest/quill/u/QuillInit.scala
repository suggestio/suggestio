package io.suggest.quill.u

import com.github.zenoamaro.react.quill.Quill
import com.quilljs.quill.modules.formats._
import com.quilljs.quill.modules.{QuillModules, QuillModulesNames}
import io.suggest.font.{MFontSizes, MFonts}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 11:11
  * Description: Начальная глобальная инициализация quill-редактора.
  */
class QuillInit {

  def fontNamesJsArray: js.Array[String] = {
    MFonts.values
      .iterator
      .map(_.cssFontFamily)
      .toJSArray
  }


  def fontSizesStringArray: js.Array[String] = {
    MFontSizes
      .values
      .iterator
      .map { mfs =>
        mfs.value.toString
      }
      .toJSArray
  }


  /** Подготовить Quill для редактора карточек. */
  def forAdEditor(): Unit = {
    // Залить список наших шрифтов в quill font format:
    val QFont = Quill.`import`[Font]( QuillModulesNames.Formats.FONT_PATH )
    QFont.whitelist = fontNamesJsArray
    Quill.register2(QFont, suppressWarnings = true)

    val QSizes = Quill.`import`[SizeClass]( QuillModulesNames.Attributors.Clazz.SIZE )
    QSizes.whitelist = fontSizesStringArray
    Quill.register2(QSizes, suppressWarnings = true)
  }


  /** Рендер JSON-а со списком модулей для текстового редактора в рекламной карточке.
    *
    * @see [[https://zenoamaro.github.io/react-quill/scripts.js]] Example js from
    *      [[https://zenoamaro.github.io/react-quill/ react-quill demo]].
    *
    * @return JSON.
    */
  def adEditorModules: js.Object = {
    // А безопасно ли тут шарить mutable instance для []?
    val noValue = js.Array[js.Any]()
    val F = QuillModulesNames.Formats

    // Конфиг всего тулбара:
    val toolbarOptions = js.Array[js.Any](

      js.Array[js.Any](
        new FontTb {
          override val font = {
            val dfltFont = MFonts.default
            MFonts
              .values
              .iterator
              .map[js.Any] { mfont =>
                if (mfont eq dfltFont) {
                  false
                } else {
                  mfont.cssFontFamily
                }
              }
              .toJSArray
          }
        },
        new SizeTb {
          override val size = {
            val dfltSz = MFontSizes.default
            MFontSizes
              .values
              .iterator
              .map[js.Any] { mfsz =>
                if (mfsz eq dfltSz)
                  false
                else
                  mfsz.value.toString
              }
              .toJSArray
          }
        }
      ),

      js.Array[js.Any](
        new AlignTb {
          override val align = noValue
        },
        F.DIRECTION
      ),

      js.Array[js.Any](
        F.BOLD,
        F.ITALIC,
        F.UNDERLINE,
        F.STRIKE
      ),

      js.Array[js.Any](
        new ColorTb {
          override val color = noValue
        },
        new BackgroundTb {
          override val background = noValue
        }
      ),

      js.Array[js.Any](
        new ScriptTb {
          override val script = Script.SUPER
        },
        new ScriptTb {
          override val script = Script.SUB
        }
      ),

      js.Array[js.Any](
        F.BLOCKQUOTE,
        F.CODE_BLOCK
      ),

      js.Array[js.Any](
        new QfListTb {
          override val list = QfList.ORDERED
        },
        new QfListTb {
          override val list = QfList.BULLET
        },
        new IndentTb {
          override val indent = Indent.MINUS_1
        },
        new IndentTb {
          override val indent = Indent.PLUS_1
        }
      ),

      js.Array[js.Any](
        F.LINK,
        F.IMAGE,
        F.VIDEO
      ),

      js.Array[js.Any](
        F.CLEAN
      )
    )

    // Собрать и вернуть итоговый конфиг.
    new QuillModules {
      override val toolbar = js.defined {
        toolbarOptions
      }
    }
  }

}
