package io.suggest.ueq

import com.quilljs.delta.Delta
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 11:01
  * Description: UnivEq support for quill types.
  */
object QuillUnivEqUtil {

  implicit def deltaUe: UnivEq[Delta] = UnivEq.force

}
