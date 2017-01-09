package util.mail

/** Интерфейс для mailer'а, приходящего через DI. */
trait IMailerWrapperDi {
  def mailer: IMailerWrapper
}
