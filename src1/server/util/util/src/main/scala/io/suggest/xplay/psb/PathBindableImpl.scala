package io.suggest.xplay.psb

import play.api.mvc.PathBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.16 11:04
  * Description: Полу-реализация PathBindable для нужд sio2.
  * PathBindable позволяет биндить значения прямо из URL path в play router.
  */
abstract class PathBindableImpl[T] extends PathBindable[T] {
}
