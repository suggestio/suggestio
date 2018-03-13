package com.quilljs.quill.modules

import com.quilljs.quill.modules.toolbar.QuillToolbarModule

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 13:56
  * Description: JSON API for quill modules configuration.
  */
trait QuillModules extends js.Object {

  /** Toolbar spec.
    *
    * For example (javascript):
    * {{{
    *   [
    *     [{ font: [] }, { size: [] }],
    *     [{ align: [] }, 'direction' ],
    *     [ 'bold', 'italic', 'underline', 'strike' ],
    *     [{ color: [] }, { background: [] }],
    *     [{ script: 'super' }, { script: 'sub' }],
    *     ['blockquote', 'code-block' ],
    *     [{ list: 'ordered' }, { list: 'bullet'}, { indent: '-1' }, { indent: '+1' }],
    *     [ 'link', 'image', 'video' ],
    *     [ 'clean' ]
    *  ]
    * }}}
    *
    */
  val toolbar: js.UndefOr[QuillToolbarModule] = js.undefined

}
