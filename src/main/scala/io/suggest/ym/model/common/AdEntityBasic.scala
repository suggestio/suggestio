package io.suggest.ym.model.common

import io.suggest.ym.model._
import io.suggest.model.common._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 16:41
 * Description: Некая сущность, участвующая в работе рекламной сети. Карточка или целый ТЦ - не суть важно.
 */

trait AdEntityBasicStatic[T <: AdEntityBasic[T]]
  extends EMDateCreatedStatic[T]
  with EMCompanyIdStatic[T]

trait AdEntityBasic[T <: AdEntityBasic[T]]
  extends EMDateCreated[T]
  with MCompanySel
  with EMCompanyId[T]

