package io.suggest.quill.u

import com.github.zenoamaro.react.quill.Quill
import com.quilljs.delta.{Delta, DeltaEmbed}
import com.quilljs.quill.core.Emitter
import com.quilljs.quill.modules.formats._
import com.quilljs.quill.modules.toolbar.{QuillToolbar, QuillToolbarModule}
import com.quilljs.quill.modules.{QuillCss, QuillModules, QuillModulesNames}
import io.suggest.common.event.DomEvents
import io.suggest.common.html.HtmlConstants
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.img.MImgFmts
import org.scalajs.dom
import org.scalajs.dom.{Event, UIEvent}
import org.scalajs.dom.raw.{FileReader, HTMLInputElement}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.{Dictionary, UndefOr}

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

    // TODO Надо настроить mime-типы. В macos диалог выбора файла не даёт выбрать SVG.
    /*
    val QImage = Quill.`import`[ImageFormat]( QuillModulesNames.Formats.IMAGE_PATH )
    QImage.accept = MImgFmts.values
      .iterator
      .flatMap(_.allMimes)
      .mkString(", ")
    Quill.register2(QImage, suppressWarnings = true)
    */
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
        val qtm = js.Object().asInstanceOf[QuillToolbarModule]
        qtm.container = toolbarOptions
        qtm.handlers = {
          // Неправильные mime-типы в accept-аттрибуте у image. Надо выставлять правильные тут:
          val imageHandler = { (quillToolbar) =>
            // Порт из themes/base.js https://github.com/quilljs/quill/blob/develop/themes/base.js#L121
            var fileInput = quillToolbar.container
              .querySelector("input.ql-image[type=file]")
              .asInstanceOf[HTMLInputElement]
            if (fileInput == null) {
              fileInput = dom.document
                .createElement( HtmlConstants.Input.input )
                .asInstanceOf[HTMLInputElement]
              fileInput.setAttribute("type", "file")
              fileInput.setAttribute("accept", MImgFmts.allMimesIter.mkString(", "))
              fileInput.classList.add( QuillCss.QL_IMAGE )

              fileInput.addEventListener( DomEvents.CHANGE, {(_: Event) =>
                if (fileInput.files != null && fileInput.files(0) != null) {
                  val reader = new FileReader()
                  reader.onload = { e: UIEvent =>
                    val range = quillToolbar.quill.getSelection(true)
                    quillToolbar.quill.updateContents(
                      new Delta()
                        .retain( range.index )
                        .delete( range.length )
                        .insert {
                          val obj = js.Object.apply().asInstanceOf[DeltaEmbed]
                          obj.image = reader.result.asInstanceOf[String]
                          obj
                        },
                      Emitter.sources.USER
                    )
                    quillToolbar.quill.setSelection(range.index + 1, Emitter.sources.SILENT)
                    fileInput.value = ""
                  }
                  // TODO По факту, base64 не нужен - надо вставлять ссылку на блоб. Потому что иначе DocEditAh потом назад конвертит это.
                  reader.readAsDataURL( fileInput.files(0) )
                }
              })
              quillToolbar.container.appendChild( fileInput )
            }
            fileInput.click()
          }: js.ThisFunction0[QuillToolbar, _]

          js.Dictionary[js.Any](
            QuillModulesNames.Formats.IMAGE -> imageHandler
          )
        }

        qtm
      }
    }
  }

}
