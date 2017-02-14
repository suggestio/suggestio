package util.mdr

import com.google.inject.{Inject, Singleton}
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.usr.MSuperUsers
import util.mail.IMailerWrapper
import views.html.sys1.mdr._mdrNeededEmailTpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.17 17:00
  * Description: Утиль для модерации.
  */
@Singleton
class MdrUtil @Inject() (
                          mailerWrapper : IMailerWrapper,
                          val mItems    : MItems,
                          val mCommonDi : ICommonDi
                        )
  extends MacroLogsImpl
{

  import mCommonDi.slick.profile.api._
  import mCommonDi.{configuration, current, ec}


  /** Кого надо уведомить о необходимости заняться модерацией? */
  val MDR_NOTIFY_EMAILS: Seq[String] = {
    val confKey = "mdr.notify.emails"
    val res = configuration.getStringList(confKey)
      .filter(!_.isEmpty)
      .fold {
        LOGGER.info(s"$confKey is undefined. Using all superusers as moderators.")
        current.injector
          .instanceOf[MSuperUsers]
          .SU_EMAILS
      } { notifyEmails =>
        LOGGER.trace(s"Successfully aquired moderators emails from $confKey")
        import scala.collection.JavaConversions._
        notifyEmails
      }
    LOGGER.info(s"Moderators are: ${res.mkString(", ")}")
    res
  }


  /** SQL для поиска нуждающихся в биллинговой модерации карточек. */
  def awaitingPaidMdrItemsSql = {
    mItems
      .query
      .filter { i =>
        i.statusStr === MItemStatuses.AwaitingMdr.strId
      }
  }


  /**
    * Выяснить у базы, требуется ли послать модераторам письмо о появлении нового платного объекта
    * в очереди на модерацию.
    * Надо вызывать перед непосредственной заливкой item'ов в базу.
    *
    * @return true/false если требуется или нет.
    */
  def isMdrNotifyNeeded: DBIOAction[Boolean, NoStream, Effect.Read] = {
    // Отправка письма потребуется, если прямо сейчас нет ни одного item'а, ожидающего модерации.
    awaitingPaidMdrItemsSql
      .exists
      .result
      .map( !_ )
  }


  /** Отправить уведомления модераторам, если требуется.
    *
    * @param isNeeded Результат экшена isMdrNotifyNeeded().
    */
  def maybeSendMdrNotify(isNeeded: Boolean): Unit = {
    if (isNeeded) {
      LOGGER.trace(s"maybeSendMdrNotify(): mdrNeeded = $isNeeded")
      sendMdrNotify()
    }
  }


  /** Отправить уведомление модератором о необходимости модерации чего-либо. */
  def sendMdrNotify(): Unit = {
    mailerWrapper
      .instance
      .setSubject("Требуется модерация")
      .setRecipients( MDR_NOTIFY_EMAILS: _* )
      .setHtml( _mdrNeededEmailTpl().body )
      .send()
  }

}
