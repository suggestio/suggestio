package io.suggest.sc.sjs.v.render.direct.res

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 16:12
 * Description: Аддон для добавления доступа к ресурсам.
 */
trait RendererResT {

  def commonRes = new CommonRes

  object focusedRes extends FocusedRes

}
