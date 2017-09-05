package com.quilljs.quill.modules

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.09.17 22:38
  * Description: Enum-like model with names for out-of-box quill modules/formats/blots/etc.
  */
object QuillModulesNames {

  object Formats {

    /** path prefix for all formats. */
    def FORMATS_ = "formats/"

    def FONT = "font"
    def FONT_PATH = FORMATS_ + FONT

    def DIRECTION = "direction"

    def BOLD = "bold"
    def ITALIC = "italic"
    def UNDERLINE = "underline"
    def STRIKE = "strike"

    def COLOR = "color"
    def BACKGROUND = "background"

    def BLOCKQUOTE = "blockquote"

    def CODE_BLOCK = "code-block"

    def INDENT = "indent"

    def LINK = "link"

    def IMAGE = "image"
    def VIDEO = "video"

    def CLEAN = "clean"

  }

}


