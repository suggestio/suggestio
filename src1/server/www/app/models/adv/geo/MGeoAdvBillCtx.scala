package models.adv.geo

import io.suggest.adv.geo.MFormS
import models.adv.{IAdvBillCtx, IAdvBillCtxWrap}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.02.17 12:05
  * Description: Модель контекста биллинга в рамках geo-биллинга.
  */
case class MGeoAdvBillCtx(
                           override val wrapped   : IAdvBillCtx,
                           res                    : MFormS
                         )
  extends IAdvBillCtxWrap

