package io.suggest.ym.model.common

import io.suggest.model.common._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:03
 * Description: Объект-участнник рекламной сети с произвольной ролью. Это может быть ТЦ, магазин или кто-то ещё.
 * Таким объектом владеют люди, и совершают действия от имени объекта.
 */

trait AdNetMemberStatic[T <: AdNetMember[T]]
  extends AdEntityBasicStatic[T]
  with EMNameStatic[T]
  with EMPersonIdsStatic[T]
  with EMLogoImgIdStatic[T]

trait AdNetMember[T <: AdNetMember[T]]
  extends AdEntityBasic[T]
  with EMName[T]
  with EMPersonIds[T]
  with EMLogoImgId[T]

