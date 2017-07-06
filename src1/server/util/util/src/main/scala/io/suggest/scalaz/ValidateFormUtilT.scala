package io.suggest.scalaz

import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 17:45
  * Description: Аналог FormUtil свалки, но с accord-валидаторами.
  */


/** Ускоренная сборка валидаторства во всяких *FormUtil. */
trait ValidateFormUtilT[T] extends IValidateDataT[T] {

  def validateFromRequest()(implicit request: Request[T]) = validateData(request.body)

}
