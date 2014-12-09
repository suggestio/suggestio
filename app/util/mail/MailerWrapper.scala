package util.mail


import javax.mail.Authenticator

import org.apache.commons.mail.{SimpleEmail, HtmlEmail, DefaultAuthenticator}
import play.api.Play.{current, configuration}
import com.typesafe.plugin.{MailerAPI, use, MailerPlugin}
import util.{AsyncUtil, PlayLazyMacroLogsImpl}

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

trait EmailBuilder {
  type T = this.type

  def setSubject(s: String): T
  def setFrom(f: String): T
  def setReplyTo(srt: String): T
  def setHtml(html: String): T
  def setText(text: String): T
  def setRecipients(rcpts: String*): T
  def send(): Unit
}


trait EmailBuilderShared extends EmailBuilder {

  protected var _html: Option[String] = None
  protected var _text: Option[String] = None

  override def setHtml(html: String): T = {
    _html = Some(html)
    this
  }
  override def setText(text: String): T = {
    _text = Some(text)
    this
  }

  protected def undefinedBodyException =
    throw new IllegalArgumentException("Nor text body, neither html body are not defined. Define at least one.")
}


class PlayMailerEmailBuilder(underlying: MailerAPI) extends EmailBuilderShared {

  override def setReplyTo(srt: String): T = {
    underlying.setReplyTo(srt)
    this
  }

  override def setFrom(f: String): T = {
    underlying.setFrom(f)
    this
  }

  override def setRecipients(rcpts: String*): T = {
    underlying.setRecipient(rcpts : _*)
    this
  }

  override def setSubject(s: String): T = {
    underlying.setSubject(s)
    this
  }

  override def send(): Unit = {
    // Нужно дергать разные методы для отправки сообщения.
    if (_html.isDefined && _text.isDefined) {
      underlying.send(_text.get, _html.get)
    } else if (_html.isDefined) {
      underlying.sendHtml(_html.get)
    } else if (_text.isDefined) {
      underlying.send(_text.get)
    } else {
      undefinedBodyException
    }
  }
}


class CommonsEmailBuilder(state: FallbackState) extends EmailBuilderShared {

  protected var _replyTo: Option[String]  = None
  protected var _recipients: Seq[String]  = Seq.empty
  protected var _from: Option[String]     = None
  protected var _subject: Option[String]  = None

  override def setReplyTo(srt: String): T = {
    _replyTo = Some(srt)
    this
  }

  override def setFrom(f: String): T = {
    _from = Some(f)
    this
  }

  override def setSubject(s: String): T = {
    _subject = Some(s)
    this
  }

  override def setRecipients(rcpts: String*): T = {
    _recipients = rcpts
    this
  }

  override def send(): Unit = {
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
    email.setHostName(state.host)
    email.setAuthenticator(state.auth)
    if (_from.isDefined)
      email.setFrom(_from.get)
    if (_subject.isDefined)
      email.setSubject(_subject.get)
    if (_replyTo.isDefined)
      email.addReplyTo(_replyTo.get)
    if (_recipients.nonEmpty)
      email.addTo(_recipients : _*)
    // Отправить собранное сообщение куда надо. Нужно освобождать текущий поток как можно скорее.
    Future {
      email.send()
    }(AsyncUtil.singleThreadIoContext)
  }

}


object MailerWrapper extends PlayLazyMacroLogsImpl {

  import LOGGER._

  def getPlayMailer: MailerAPI = use[MailerPlugin].email

  /** Неизменяемая резидентная инфа по fallback'у. */
  private lazy val fallBackInfo: FallbackState = {
    val username = configuration.getString("smtp.user").get
    val password = configuration.getString("smtp.password").get
    FallbackState(
      auth = new DefaultAuthenticator(username, password),
      host = configuration.getString("smtp.host").get
    )
  }

  val usePlayMailer: Boolean = {
    try {
      getPlayMailer
      true
    } catch {
      case ex: Exception =>
        warn("Cannot use play-mailer because of exception while instantiating", ex)
        false
    }
  }


  def instance: EmailBuilder = {
    if (usePlayMailer) {
      new PlayMailerEmailBuilder(getPlayMailer)
    } else {
      new CommonsEmailBuilder(fallBackInfo)
    }
  }


}


sealed case class FallbackState(auth: Authenticator, host: String)

