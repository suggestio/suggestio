package com.quilljs.quill.modules

import com.quilljs.quill.modules.formats.Size

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.09.17 22:38
  * Description: Enum-like model with names for out-of-box quill modules/formats/blots/etc.
  */
object QuillModulesNames {

  private final def DELIM = "/"

  object Modules {
    final def MODULES_ = "modules" + DELIM

    final def TABLE = "table"
    final def TOOLBAR = "toolbar"

  }

  object Formats {

    /** path prefix for all formats. */
    final def FORMATS_ = "formats" + DELIM

    final def FONT = "font"
    final def FONT_PATH = FORMATS_ + FONT

    final def DIRECTION = "direction"

    final def BOLD = "bold"
    final def ITALIC = "italic"
    final def UNDERLINE = "underline"
    final def STRIKE = "strike"

    final def COLOR = "color"
    final def BACKGROUND = "background"

    final def BLOCKQUOTE = "blockquote"

    final def CODE_BLOCK = "code-block"

    final def INDENT = "indent"

    final def LINK = "link"

    final def IMAGE = "image"
    final def IMAGE_PATH = FORMATS_ + IMAGE
    final def VIDEO = "video"

    final def FORMULA = "formula"

    final def CLEAN = "clean"

  }

  object Attributors {
    final def ATTRIBUTORS = "attributors"

    object Style {
      final def STYLE = "style"
      final def SIZE = ATTRIBUTORS + DELIM + STYLE + DELIM + Size.SIZE
    }

    object Clazz {
      final def CLAZZ = "class"
      final def SIZE = ATTRIBUTORS + DELIM + CLAZZ + DELIM + Size.SIZE
    }

  }

}


