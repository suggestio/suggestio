package models.req

import models.mext.ILoginProvider
import play.api.mvc.Request
import util.ident.IExtLoginAdp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.02.19 21:22
  * Description: Модель реквест для экшенов логина через внешний сервис/систему.
  */
case class MLoginViaReq[A](
                            apiAdp                  : IExtLoginAdp,
                            loginProvider           : ILoginProvider,
                            override val request    : Request[A],
                            override val user       : ISioUser,
                          )
  extends MReqWrap[A]

