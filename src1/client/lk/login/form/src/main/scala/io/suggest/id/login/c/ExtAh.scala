package io.suggest.id.login.c

import diode.data.Pot
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode._
import diode.Implicits._
import io.suggest.id.login.m.{ExtLoginVia, ExtLoginViaTimeout, LoginFormDiConfig}
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.routes.routes
import io.suggest.sjs.dom2.DomQuick
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 17:44
  * Description: Контроллер данных для внешнего логина.
  */
class ExtAh[M](
                modelRW       : ModelRW[M, MExtLoginFormS],
                returnUrlRO   : ModelRO[Option[String]],
                diConfig      : LoginFormDiConfig,
              )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запуск логина через внешний сервис. Надо начать запрос ссылки.
    case m: ExtLoginVia =>
      val v0 = value
      if (v0.loginUrlReq.isPending) {
        noChange
      } else {
        val tstamp = System.currentTimeMillis()
        val fx = diConfig.onRedirect( m,
          external = true,
          rdrUrl = routes.controllers.Ident.idViaProvider( m.service.value, returnUrlRO.value.toUndef ).url
        ) >> Effect
          .action ( ExtLoginViaTimeout(tstamp) )
          .after(3000.milliseconds)

        val v2 = MExtLoginFormS.loginUrlReq.modify(_.pending(tstamp))(v0)
        updated( v2, fx )
      }


    // Логин не удался - разблокировать кнопку.
    case _: ExtLoginViaTimeout =>
      val v0 = value
      val v2 = MExtLoginFormS.loginUrlReq.replace( Pot.empty )(v0)
      updated( v2 )

  }

}
