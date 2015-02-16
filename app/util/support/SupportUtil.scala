package util.support

import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.02.15 17:01
 * Description: Утиль для фидбэков и прочей обратной связи.
 */
object SupportUtil {

  val FEEDBACK_RCVR_EMAILS = configuration.getStringSeq("feedback.send.to.emails") getOrElse Seq("support@suggest.io")

}
