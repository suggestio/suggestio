package io.suggest.lk.dt.interval.vm

import io.suggest.dt.interval.DatesIntervalConstants
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.find.FindDiv

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 23:11
 * Description: vm'ка контейнера инфы по датам.
 * В новой (этой) архитектуре оно всегда рендерится на сервере.
 * В mx_cof оно кое-как рендерилось на клиенте.
 */
object InfoContainer extends FindDiv {

  override type T     = InfoContainer
  override def DOM_ID = DatesIntervalConstants.INFO_CONT_ID
}


import InfoContainer.Dom_t


trait InfoContainerT extends SetInnerHtml {
  override type T = Dom_t
}


case class InfoContainer(override val _underlying: Dom_t)
  extends InfoContainerT
