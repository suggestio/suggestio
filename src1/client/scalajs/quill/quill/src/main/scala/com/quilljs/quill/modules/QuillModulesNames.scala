package com.quilljs.quill.modules

import com.quilljs.quill.modules.formats.Size

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.09.17 22:38
  * Description: Enum-like model with names for out-of-box quill modules/formats/blots/etc.
  */
object QuillModulesNames {

  private def DELIM = "/"

  object Formats {

    /** path prefix for all formats. */
    def FORMATS_ = "formats" + DELIM

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

  object Attributors {
    def ATTRIBUTORS = "attributors"

    object Style {
      def STYLE = "style"
      def SIZE = ATTRIBUTORS + DELIM + STYLE + DELIM + Size.SIZE
    }

    object Clazz {
      def CLAZZ = "class"
      def SIZE = ATTRIBUTORS + DELIM + CLAZZ + DELIM + Size.SIZE
    }

  }

}


