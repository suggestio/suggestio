package util.mail

import javax.mail.Authenticator

import com.google.inject.assistedinject.Assisted
import com.google.inject.{ImplementedBy, Inject, Singleton}
import io.suggest.async.AsyncUtil
import io.suggest.util.logs.MacroLogsImplLazy
import org.apache.commons.mail.{DefaultAuthenticator, HtmlEmail, SimpleEmail}
import play.api.Configuration
import play.api.inject.Injector
import play.api.libs.mailer.{Email, MailerClient}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.12.14 17:18
 * Description: В связи с тем, что play-plugins-mailer был выкинут из официальной поддержки и живёт сам по себе,
 * нужно иметь в наличии запасной метод отправки email-сообщений.
 * Используется commons-email как запасной, на случай если исходный метод не работает.
 * play-mailer тоже использует commons, поэтому доп.зависимостей не требуется.
 * @see [[https://commons.apache.org/proper/commons-email/userguide.html]]
 */

object MailerWrapper {

  def SUBJECT_DFLT = "(No subject)"

  /** Основной адресок почты, чтобы везде setFrom() не вызывать. */
  def EMAIL_NO_REPLY = "no-reply@suggest.io"

}

import MailerWrapper._


/** Класс-сырец для билдера писем. */
sealed abstract class EmailBuilder extends MacroLogsImplLazy {

  type T = this.type

  def setSubject(s: String): T
  def setFrom(f: String): T
  def setReplyTo(srt: String): T
  def setHtml(html: String): T
  def setText(text: String): T
  def setRecipients(rcpts: String*): T

  /**
    * Полностью асинхронная отправка сообщения.
    * Тело реализуемой логики должно исполняться асихронно в принудительном порядке:
    * это избавит от задержек в исполнении основных экшенов и защитит от ошибок.
    *
    * @return Фьючерс с неопределённым значением любого типа.
    */
  final def send(): Future[_] = {
    val fut = Future {
      _doSend()
    }
      .flatMap(identity)
    fut.onFailure { case ex: Throwable =>
      LOGGER.error("Failed to send email message:\n" + this, ex)
    }
    fut
  }

  /** Реализация отправки письма. Логика эта обычно синхронная, но это тут не важно. */
  protected def _doSend(): Future[_]

}


/** Абстрактная реализация set-методов [[EmailBuilder]], которая работает в виде билдера,
  * который накапливает все данные у себя в состоянии. Она потоко-НЕбезопасна. */
sealed abstract class EmailBuilderShared extends EmailBuilder {

  protected var _html: Option[String]     = None
  protected var _text: Option[String]     = None
  protected var _replyTo: Option[String]  = None
  protected var _recipients: Seq[String]  = Nil
  protected var _from: Option[String]     = None
  protected var _subject: Option[String]  = None


  override def setHtml(html: String): T = {
    _html = Some(html)
    this
  }
  override def setText(text: String): T = {
    _text = Some(text)
    this
  }

  override def setReplyTo(srt: String): T = {
    _replyTo = Some(srt)
    this
  }

  override def setFrom(f: String): T = {
    _from = Some(f)
    this
  }
  protected def _getFrom: String = _from.getOrElse(EMAIL_NO_REPLY)

  override def setSubject(s: String): T = {
    _subject = Some(s)
    this
  }
  protected def _getSubject = _subject.getOrElse(SUBJECT_DFLT)

  override def setRecipients(rcpts: String*): T = {
    _recipients = rcpts
    this
  }

  protected def undefinedBodyException =
    throw new IllegalArgumentException("Nor text body, neither html body are not defined. Define at least one.")
}


/** Билдер для play-mailer'а. */
class PlayMailerEmailBuilder @Inject() (
  mailClient        : MailerClient
)
  extends EmailBuilderShared
{


  /** Реализация отправки письма. Логика эта обычно синхронная, но это тут не важно. */
  override protected def _doSend(): Future[_] = {
    val email = Email(
      subject   = _getSubject,
      from      = _getFrom,
      to        = _recipients,
      bodyText  = _text,
      bodyHtml  = _html,
      replyTo   = _replyTo
    )
    val mailId = mailClient.send(email)
    Future.successful(mailId)
  }

}
/** Интерфейс для Guice factory, собирающей инстансы [[PlayMailerEmailBuilder]]. */
trait PlayMailerEmailBuildersFactory {
  def create(): PlayMailerEmailBuilder
}


/** Если play-mailer не работает, то можно использовать вот это. */
class CommonsEmailBuilder @Inject() (
  @Assisted state : FallbackState,
  asyncUtil       : AsyncUtil
)
  extends EmailBuilderShared
  with MacroLogsImplLazy
{

  /** Реализация отправки письма. Логика эта обычно синхронная, но это тут не важно. */
  override protected def _doSend(): Future[_] = {
    // В зависимости от наличия/отсутствия html-части нужно дергать те или иные классы:
    val email = if (_html.isDefined) {
      val _email = new HtmlEmail()
      _email.setHtmlMsg(_html.get)
      if (_text.isDefined)
        _email.setTextMsg(_text.get)
      _email
    } else if (_text.isDefined) {
      val _email = new SimpleEmail()
      _email.setMsg(_text.get)
      _email
    } else {
      undefinedBodyException
    }

    // Расставить заголовки сообщения.
    email.setHostName( state.host )
    email.setAuthenticator( state.auth )
    email.setFrom( _getFrom )
    email.setSubject( _getSubject )
    for (r <- _replyTo)
      email.addReplyTo(r)
    if (_recipients.nonEmpty)
      email.addTo(_recipients : _*)

    // Отправить собранное сообщение куда надо. Нужно освобождать текущий поток как можно скорее.
    Future {
      email.send()
    }(asyncUtil.singleThreadIoContext)
  }

}
/** Интерфейс Guice factory для сборки инстансов [[CommonsEmailBuilder]]. */
trait CommonsEmailBuildersFactory {
  def create(state: FallbackState): CommonsEmailBuilder
}


@ImplementedBy(classOf[MailerWrapper])
trait IMailerWrapper {

  def instance: EmailBuilder

  /** Адреса получателей, если требуется отправлять письмо программерам. */
  final def EMAILS_PROGRAMMERS = "konstantin.nikiforov@cbca.ru" :: Nil

}

@Singleton
class MailerWrapper @Inject() (
  configuration : Configuration,
  injector      : Injector
)
  extends IMailerWrapper
{

  /** Использовать ли play mailer для отправки электронной почты? */
  val USE_PLAY_MAILER = configuration.getBoolean("email.use.play.mailer").getOrElse(true)

  private lazy val _playEmailFactory    = injector.instanceOf[PlayMailerEmailBuildersFactory]
  private lazy val _commonsEmailFactory = injector.instanceOf[CommonsEmailBuildersFactory]

  /** Неизменяемая резидентная инфа по fallback'у. */
  private lazy val fallBackInfo: FallbackState = {
    val username = configuration.getString("smtp.user").get
    val password = configuration.getString("smtp.password").get
    FallbackState(
      auth = new DefaultAuthenticator(username, password),
      host = configuration.getString("smtp.host").get
    )
  }

  /** Выдать инстанс EmailBuilder'а, который позволит собрать письмо и отправить. */
  def instance: EmailBuilder = {
    if (USE_PLAY_MAILER) {
      _playEmailFactory.create()
    } else {
      _commonsEmailFactory.create(fallBackInfo)
    }
  }

}


sealed case class FallbackState(auth: Authenticator, host: String)

