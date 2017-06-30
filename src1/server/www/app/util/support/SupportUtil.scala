package util.support

import javax.inject.Inject
import play.api.Configuration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.02.15 17:01
 * Description: Утиль для фидбэков и прочей обратной связи.
 */
class SupportUtil @Inject() (configuration: Configuration) {

  /** Адреса email, на которые будет отправлено сообщение от юзера.
    *
    * 2017.apr.18: Первый в списке мыльник будет отображаться на странице /lk/support,
    * поэтому на продакшене первый мыльник должен быть 100% публичным. См. [[views.html.lk.support.supportFormTpl]].
    */
  val FEEDBACK_RCVR_EMAILS: Seq[String] = {
    configuration.getOptional[Seq[String]]("feedback.send.to.emails")
      .getOrElse( "support@suggest.io" :: Nil )
  }

}
