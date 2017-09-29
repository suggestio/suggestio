package com.github.zenoamaro.react.quill

import com.quilljs.quill.QuillStatic

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.09.17 22:28
  * Description: react-examples showing imports for static Quill from 'react-quill' (not from 'quill').
  * It is a Quill instance, used by ReactQuill.
  */
@JSImport("react-quill", "Quill")
@js.native
object Quill extends QuillStatic
