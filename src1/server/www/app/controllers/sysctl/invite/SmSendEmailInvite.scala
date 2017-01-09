package controllers.sysctl.invite

import controllers.SioController
import models.mctx.Context
import models.req.IReqHdr
import models.usr.EmailActivation
import models.{AdnShownTypes, _}
import util.mail.IMailerWrapperDi
import views.html.lk.adn.invite.emailNodeOwnerInviteTpl

/** Утиль для контроллеров для отправки письма с доступом на узел. */
trait SmSendEmailInvite
  extends SioController
  with IMailerWrapperDi
{

  import mCommonDi._

  /** Выслать письмо активации. */
  def sendEmailInvite(ea: EmailActivation, adnNode: MNode)(implicit request: IReqHdr) {
    // Собираем и отправляем письмо адресату
    val msg = mailer.instance
    implicit val ctx = implicitly[Context]

    val ast = adnNode.extras.adn
      .flatMap( _.shownTypeIdOpt )
      .flatMap( AdnShownTypes.maybeWithName )
      .getOrElse( AdnShownTypes.default )

    msg.setSubject("Suggest.io | " +
      ctx.messages("Your") + " " +
      ctx.messages(ast.singular)
    )
    msg.setFrom("no-reply@suggest.io")
    msg.setRecipients(ea.email)
    msg.setHtml {
      htmlCompressUtil.html4email {
        emailNodeOwnerInviteTpl(adnNode, ea)(ctx)
      }
    }
    msg.send()
  }

}
